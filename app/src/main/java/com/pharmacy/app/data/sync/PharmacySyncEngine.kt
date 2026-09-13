package com.pharmacy.app.data.sync

import android.content.Context
import android.util.Log
import com.pharmacy.app.data.MedicineEntity
import com.pharmacy.app.data.PharmacyDao
import com.pharmacy.app.data.SaleRecordEntity
import com.pharmacy.app.data.supabase.SupabaseAuthManager
import com.pharmacy.app.data.supabase.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * حالة المزامنة اللحظية
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

/**
 * محرك المزامنة التلقائية (Sync Engine)
 * - يدعم Offline-First: العمليات تُكتب محلياً في Room أولاً ثم تُدرج في قائمة الانتظار
 * - يراقب عودة الإنترنت لرفع العمليات المتراكمة بالترتيب إلى Supabase
 * - يمسح كل عملية من قائمة الانتظار فقط بعد تأكيد رفعها بنجاح
 * - يطبق تراجعاً أسياً (Exponential Backoff) عند الفشل
 * - يدعم التنزيل الأولي (Initial Pull) للبيانات عند تسجيل الدخول على جهاز جديد
 */
class PharmacySyncEngine(
    private val context: Context,
    private val pharmacyDao: PharmacyDao,
    private val syncQueueDao: SyncQueueDao,
    private val authManager: SupabaseAuthManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityMonitor = NetworkConnectivityMonitor(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val pendingCountFlow = syncQueueDao.getPendingCountFlow()

    init {
        // مراقبة اتصال الشبكة: عند عودة الإنترنت، تشغيل المزامنة فوراً
        scope.launch {
            connectivityMonitor.isOnline.collectLatest { online ->
                if (online && authManager.hasValidLocalSession() && SupabaseConfig.isConfigured) {
                    processPendingQueue()
                }
            }
        }
    }

    /**
     * إدراج عملية في قائمة الانتظار وتشغيل المزامنة إذا كان الإنترنت متوفراً
     */
    suspend fun enqueueOperation(
        operationType: SyncOperationType,
        entityType: String,
        localId: Long,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        val entity = SyncQueueEntity(
            operationType = operationType.name,
            entityType = entityType,
            localId = localId,
            payloadJson = payloadJson
        )
        syncQueueDao.enqueue(entity)

        // محاولة المزامنة الفورية إذا كان هناك إنترنت
        if (connectivityMonitor.isCurrentlyConnected()) {
            scope.launch {
                processPendingQueue()
            }
        }
    }

    /**
     * معالجة العمليات المتراكمة في قائمة الانتظار بالتسلسل
     */
    suspend fun processPendingQueue(): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        val session = authManager.currentSession.value ?: return@withContext false

        val pending = syncQueueDao.getAllPending()
        if (pending.isEmpty()) {
            _syncStatus.value = SyncStatus.Idle
            return@withContext true
        }

        _syncStatus.value = SyncStatus.Syncing

        var allSuccessful = true
        for (item in pending) {
            val success = executeSyncItemWithRetry(item, session.accessToken, session.userId)
            if (success) {
                syncQueueDao.deleteById(item.id)
            } else {
                syncQueueDao.incrementRetryCount(item.id)
                allSuccessful = false
                break // التوقف للحفاظ على الترتيب وتجنب الفوضى عند انقطاع الاتصال
            }
        }

        val remainingCount = syncQueueDao.getPendingCount()
        _syncStatus.value = if (allSuccessful && remainingCount == 0) {
            SyncStatus.Success("تمت مزامنة جميع البيانات بنجاح مع السحابة")
        } else {
            SyncStatus.Error("تعذر رفع بعض العمليات، ستتم إعادة المحاولة تلقائياً")
        }

        allSuccessful
    }

    /**
     * تنفيذ عنصر واحد من قائمة الانتظار مع إعادة المحاولة بتراجع أسي (Exponential Backoff)
     */
    private suspend fun executeSyncItemWithRetry(
        item: SyncQueueEntity,
        accessToken: String,
        userId: String
    ): Boolean {
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts) {
            try {
                val ok = executeSingleSyncRequest(item, accessToken, userId)
                if (ok) return true
            } catch (e: Exception) {
                Log.w("PharmacySyncEngine", "Attempt ${attempts + 1} failed for item ${item.id}: ${e.message}")
            }
            attempts++
            if (attempts < maxAttempts) {
                val backoffMs = (2.0.pow(attempts.toDouble()) * 800).toLong()
                delay(min(backoffMs, 5000L))
            }
        }
        return false
    }

    private suspend fun executeSingleSyncRequest(
        item: SyncQueueEntity,
        accessToken: String,
        userId: String
    ): Boolean {
        val opType = try {
            SyncOperationType.valueOf(item.operationType)
        } catch (e: Exception) {
            return true // تخطي العناصر غير الصالحة
        }

        return when (opType) {
            SyncOperationType.UPSERT_MEDICINE -> {
                val jsonObj = JSONObject(item.payloadJson)
                jsonObj.put("user_id", userId)
                jsonObj.put("is_deleted", false)

                val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/medicines?on_conflict=user_id,local_id"
                postUpsertToSupabase(url, jsonObj, accessToken)
            }

            SyncOperationType.DELETE_MEDICINE -> {
                val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/medicines?local_id=eq.${item.localId}&user_id=eq.$userId"
                // Soft delete
                val updateObj = JSONObject().apply {
                    put("is_deleted", true)
                }
                patchToSupabase(url, updateObj, accessToken)
            }

            SyncOperationType.INSERT_SALE -> {
                val jsonObj = JSONObject(item.payloadJson)
                jsonObj.put("user_id", userId)
                jsonObj.put("is_deleted", false)

                val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/sales?on_conflict=user_id,local_id"
                postUpsertToSupabase(url, jsonObj, accessToken)
            }

            SyncOperationType.DELETE_INVOICE -> {
                val jsonObj = JSONObject(item.payloadJson)
                val invoiceId = jsonObj.optString("invoiceId", "")
                if (invoiceId.isNotBlank()) {
                    val url = "${SupabaseConfig.SUPABASE_URL}/rest/v1/sales?invoice_id=eq.$invoiceId&user_id=eq.$userId"
                    val updateObj = JSONObject().apply {
                        put("is_deleted", true)
                    }
                    patchToSupabase(url, updateObj, accessToken)
                } else true
            }
        }
    }

    private fun postUpsertToSupabase(url: String, payload: JSONObject, accessToken: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${if (accessToken.isNotBlank()) accessToken else SupabaseConfig.SUPABASE_KEY}")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    private fun patchToSupabase(url: String, payload: JSONObject, accessToken: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
            .addHeader("Authorization", "Bearer ${if (accessToken.isNotBlank()) accessToken else SupabaseConfig.SUPABASE_KEY}")
            .addHeader("Prefer", "return=minimal")
            .addHeader("Content-Type", "application/json")
            .patch(payload.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    /**
     * تنزيل بيانات الصيدلية من السحابة إلى القاعدة المحلية عند تسجيل الدخول على جهاز جديد (Initial Pull)
     */
    suspend fun pullDataFromCloud(): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) return@withContext false
        val session = authManager.currentSession.value ?: return@withContext false

        _syncStatus.value = SyncStatus.Syncing

        try {
            // 1. استرجاع الأدوية النشطة
            val medicinesUrl = "${SupabaseConfig.SUPABASE_URL}/rest/v1/medicines?user_id=eq.${session.userId}&is_deleted=eq.false&select=*"
            val medRequest = Request.Builder()
                .url(medicinesUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken.ifBlank { SupabaseConfig.SUPABASE_KEY }}")
                .get()
                .build()

            val medResponse = client.newCall(medRequest).execute()
            if (medResponse.isSuccessful) {
                val medBody = medResponse.body?.string() ?: "[]"
                val jsonArray = JSONArray(medBody)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val localId = obj.optLong("local_id", 0L)
                    val entity = MedicineEntity(
                        id = if (localId > 0) localId else 0L,
                        name = obj.optString("name", ""),
                        barcode = obj.optString("barcode", ""),
                        buyPrice = obj.optDouble("buy_price", 0.0),
                        sellPrice = obj.optDouble("sell_price", 0.0),
                        quantity = obj.optInt("quantity", 0),
                        minStockAlert = obj.optInt("min_stock_alert", 5),
                        category = obj.optString("category", "أدوية عامة"),
                        location = obj.optString("location", ""),
                        expiryDate = obj.optLong("expiry_date", 0L),
                        batchesJson = obj.optString("batches_json", "[]")
                    )
                    pharmacyDao.insertMedicine(entity)
                }
            }

            // 2. استرجاع سجل المبيعات
            val salesUrl = "${SupabaseConfig.SUPABASE_URL}/rest/v1/sales?user_id=eq.${session.userId}&is_deleted=eq.false&select=*"
            val salesRequest = Request.Builder()
                .url(salesUrl)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken.ifBlank { SupabaseConfig.SUPABASE_KEY }}")
                .get()
                .build()

            val salesResponse = client.newCall(salesRequest).execute()
            if (salesResponse.isSuccessful) {
                val salesBody = salesResponse.body?.string() ?: "[]"
                val salesArray = JSONArray(salesBody)
                for (i in 0 until salesArray.length()) {
                    val obj = salesArray.getJSONObject(i)
                    val localId = obj.optLong("local_id", 0L)
                    val saleEntity = SaleRecordEntity(
                        id = if (localId > 0) localId else 0L,
                        invoiceId = obj.optString("invoice_id", ""),
                        medicineId = obj.optLong("medicine_id", 0L),
                        medicineName = obj.optString("medicine_name", ""),
                        barcode = obj.optString("barcode", ""),
                        quantitySold = obj.optInt("quantity_sold", 1),
                        unitCostPrice = obj.optDouble("unit_cost_price", 0.0),
                        unitSellPrice = obj.optDouble("unit_sell_price", 0.0),
                        totalSellPrice = obj.optDouble("total_sell_price", 0.0),
                        totalProfit = obj.optDouble("total_profit", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    pharmacyDao.insertSale(saleEntity)
                }
            }

            _syncStatus.value = SyncStatus.Success("تم تنزيل وتحديث البيانات السحابية بنجاح")
            true
        } catch (e: Exception) {
            Log.e("PharmacySyncEngine", "Pull from cloud failed: ${e.message}", e)
            _syncStatus.value = SyncStatus.Error("تعذر تنزيل البيانات من السحابة، يتم العمل بالبيانات المحلية")
            false
        }
    }
}
