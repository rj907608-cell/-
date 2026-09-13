package com.pharmacy.app.data.supabase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * مدير المصادقة وحفظ الجلسات مع Supabase Auth
 * - متوافق مع مبدأ Offline-First: يقرأ الجلسة المحلية فورياً
 * - يحفظ الجلسة محلياً لعدم المطالبة بتسجيل الدخول في المرات القادمة
 * - يجدد رمز الدخول بصمت عند توفر الإنترنت
 * - يستمر بالعمل دون إنترنت إذا كانت هناك جلسة مسجلة سابقاً
 */
class SupabaseAuthManager(context: Context) {

    init {
        SupabaseConfig.init(context)
    }

    private val prefs = context.getSharedPreferences("supabase_auth_secure_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _currentSession = MutableStateFlow<SupabaseUserSession?>(loadSavedSession())
    val currentSession: StateFlow<SupabaseUserSession?> = _currentSession.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(hasValidLocalSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private fun loadSavedSession(): SupabaseUserSession? {
        val savedJson = prefs.getString(PREF_SESSION_KEY, null) ?: return null
        return SupabaseUserSession.fromJson(savedJson)
    }

    fun hasValidLocalSession(): Boolean {
        val session = _currentSession.value ?: loadSavedSession() ?: return false
        return session.userId.isNotBlank()
    }

    private fun saveSession(session: SupabaseUserSession) {
        _currentSession.value = session
        _isLoggedIn.value = true
        prefs.edit()
            .putString(PREF_SESSION_KEY, session.toJson())
            .putBoolean(PREF_HAS_LOGGED_IN_BEFORE, true)
            .apply()
    }

    fun clearSession() {
        _currentSession.value = null
        _isLoggedIn.value = false
        prefs.edit().remove(PREF_SESSION_KEY).apply()
    }

    /**
     * تسجيل الخروج وإلغاء الجلسة محلياً وسحابياً
     */
    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (session != null && SupabaseConfig.isConfigured && session.accessToken.isNotBlank()) {
            try {
                val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/logout"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer ${session.accessToken}")
                    .post("{}".toRequestBody(jsonMediaType))
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) {
                Log.w("SupabaseAuth", "Remote logout call failed: ${e.message}")
            }
        }
        clearSession()
        clearPendingAccount()
        true
    }

    /**
     * التحقق من أن حساب المستخدم ما زال موجوداً ومفعلاً على خادم Supabase
     * إذا تم حذف الحساب من قائمة Users في لوحة التحكم، فسيتم إرجاع خطأ 401/403 أو 400
     * وعندها يتم تلقائياً تسجيل الخروج وحذف الجلسة المحلية فوراً.
     * يرجع true إذا كان الحساب سليم وموجود، false إذا تم حذفه من السيرفر، null إذا كان فحص الشبكة غير متاح (أوفلاين)
     */
    suspend fun validateSessionWithServer(): Boolean? = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: loadSavedSession() ?: return@withContext false
        if (!SupabaseConfig.isConfigured || session.accessToken.isBlank() || session.userId.startsWith("offline_")) {
            return@withContext null
        }

        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/user"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val code = response.code

            // إذا أرجع 200 OK فالمستخدم موجود ومفعل
            if (response.isSuccessful) {
                return@withContext true
            }

            // إذا انتهت صلاحية التوكن أو غير صالح، نحاول تجديده
            if (code == 401 || code == 403 || code == 400) {
                if (session.refreshToken.isNotBlank()) {
                    val refreshOk = refreshSessionSilently()
                    if (refreshOk) {
                        return@withContext true
                    }
                }
                // الحساب تم حذفه من لوحة تحكم Supabase Users أو تم إلغاؤه
                Log.w("SupabaseAuth", "User account removed or invalid on Supabase (HTTP $code). Logging out.")
                withContext(Dispatchers.Main) {
                    clearSession()
                }
                return@withContext false
            }

            return@withContext null
        } catch (e: IOException) {
            // انقطاع إنترنت عادي
            return@withContext null
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Error verifying user session", e)
            return@withContext null
        }
    }

    /**
     * إنشاء جلسة محلية للعمل بنمط أوفلاين في حال لم يتم تهيئة سحابة Supabase بعد
     */
    fun createLocalOfflineSession(email: String, name: String): SupabaseUserSession {
        val session = SupabaseUserSession(
            accessToken = "offline_local_token",
            refreshToken = "offline_local_refresh",
            userId = "offline_${System.currentTimeMillis()}",
            email = email.ifBlank { "offline@pharmacy.local" },
            name = name.ifBlank { "صيدلية محلية" },
            expiresAt = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
        )
        saveSession(session)
        return session
    }

    /**
     * تسجيل حساب جديد عبر Supabase Auth (Sign Up)
     */
    suspend fun signUp(email: String, password: String, name: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext AuthResult.Error("لم يتم ربط التطبيق بمشروع Supabase الخاص بك بعد. (اضغط 5 مرات على شعار الصيدلية في الأعلى لإدخال رابط المشروع ومفتاحك، أو أرسلهما في المحادثة لربطهما تلقائياً)")
        }

        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/signup"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                val dataObj = JSONObject().apply {
                    put("display_name", name.trim())
                    put("name", name.trim())
                    put("is_approved", false)
                    put("approved", false)
                    put("status", "pending")
                }
                put("data", dataObj)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                return@withContext AuthResult.Error(errorMsg)
            }

            val jsonObj = JSONObject(responseBody)
            val accessToken = jsonObj.optString("access_token", "")
            val refreshToken = jsonObj.optString("refresh_token", "")
            val expiresIn = jsonObj.optLong("expires_in", 3600L)
            val expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn

            val userObj = jsonObj.optJSONObject("user")
            val userId = userObj?.optString("id") ?: jsonObj.optString("id", "")
            val userEmail = userObj?.optString("email") ?: email

            // عند إنشاء الحساب، يذهب الحساب دائماً إلى حالة الانتظار حتى يقوم المدير بتأكيده من لوحة تحكم Supabase Users
            return@withContext AuthResult.RequiresEmailVerification(
                email = email,
                message = "تم استلام بيانات تسجيلك بنجاح. حسابك بانتظار الاعتماد والتفعيل من قبل الإدارة."
            )
        } catch (e: IOException) {
            Log.e("SupabaseAuth", "Network error during signUp", e)
            return@withContext AuthResult.Error("تعذر الاتصال بالخادم. يرجى التأكد من اتصال الإنترنت.")
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Unexpected error during signUp", e)
            return@withContext AuthResult.Error(e.localizedMessage ?: "حدث خطأ غير متوقع أثناء التسجيل")
        }
    }

    /**
     * التحقق من رمز OTP المكون من 6 أرقام (Verify OTP)
     */
    suspend fun verifyOtp(email: String, token: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext AuthResult.Error("إعدادات الاتصال بـ Supabase غير مهيأة")
        }

        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/verify"
            val payload = JSONObject().apply {
                put("type", "signup") // أو email
                put("email", email.trim())
                put("token", token.trim())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // تجربة type = email كبديل إذا كان النوع مختلفاً
                val retryPayload = JSONObject().apply {
                    put("type", "email")
                    put("email", email.trim())
                    put("token", token.trim())
                }
                val retryRequest = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(retryPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                val retryResponse = client.newCall(retryRequest).execute()
                val retryBody = retryResponse.body?.string() ?: ""

                if (!retryResponse.isSuccessful) {
                    val errorMsg = parseErrorMessage(retryBody.ifBlank { responseBody })
                    return@withContext AuthResult.Error(errorMsg)
                }
                return@withContext handleAuthSuccess(retryBody, email)
            }

            return@withContext handleAuthSuccess(responseBody, email)
        } catch (e: IOException) {
            return@withContext AuthResult.Error("تعذر الاتصال بالشبكة للتحقق من الرمز.")
        } catch (e: Exception) {
            return@withContext AuthResult.Error(e.localizedMessage ?: "حدث خطأ أثناء التحقق من الرمز")
        }
    }

    /**
     * تسجيل الدخول بالبريد وكلمة المرور (Sign In with Password)
     */
    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            val pending = getPendingAccount()
            if (pending != null && pending.email.equals(email.trim(), ignoreCase = true)) {
                return@withContext AuthResult.Error("الحساب ما زال بانتظار التفعيل والاعتماد من قبل الإدارة. يرجى التواصل مع الدعم.")
            }
            return@withContext AuthResult.Error("البريد الإلكتروني أو كلمة المرور غير صحيحة، أو أن الحساب لم يتم تفعيله بعد.")
        }

        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody)
                return@withContext AuthResult.Error(errorMsg)
            }

            return@withContext handleAuthSuccess(responseBody, email)
        } catch (e: IOException) {
            // فحص إذا كان هناك جلسة سابقة للعمل أوفلاين
            if (hasValidLocalSession()) {
                val existing = _currentSession.value ?: loadSavedSession()!!
                return@withContext AuthResult.Success(existing)
            }
            return@withContext AuthResult.Error("تعذر الاتصال بالإنترنت لتسجيل الدخول الأول.")
        } catch (e: Exception) {
            return@withContext AuthResult.Error(e.localizedMessage ?: "فشل تسجيل الدخول")
        }
    }

    /**
     * تجديد رمز الجلسة بصمت في الخلفية عند الحاجة
     */
    suspend fun refreshSessionSilently(): Boolean = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: loadSavedSession() ?: return@withContext false
        if (session.refreshToken.isBlank() || !SupabaseConfig.isConfigured) return@withContext false

        try {
            val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply {
                put("refresh_token", session.refreshToken)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.SUPABASE_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val jsonObj = JSONObject(body)
                val newAccessToken = jsonObj.optString("access_token", session.accessToken)
                val newRefreshToken = jsonObj.optString("refresh_token", session.refreshToken)
                val expiresIn = jsonObj.optLong("expires_in", 3600L)
                val expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn

                val updatedSession = session.copy(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    expiresAt = expiresAt
                )
                saveSession(updatedSession)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuth", "Silent refresh failed (offline or transient): ${e.message}")
        }
        return@withContext false
    }

    private fun handleAuthSuccess(responseBody: String, defaultEmail: String): AuthResult {
        return try {
            val jsonObj = JSONObject(responseBody)
            val accessToken = jsonObj.optString("access_token", "")
            val refreshToken = jsonObj.optString("refresh_token", "")
            val expiresIn = jsonObj.optLong("expires_in", 3600L)
            val expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn

            val userObj = jsonObj.optJSONObject("user")
            val userId = userObj?.optString("id") ?: jsonObj.optString("id", "")
            val userEmail = userObj?.optString("email") ?: defaultEmail
            val userMetadata = userObj?.optJSONObject("user_metadata")
            val name = userMetadata?.optString("name") ?: userMetadata?.optString("display_name") ?: ""

            // التحقق الصارم مما إذا كان الحساب قد تم تأكيده واعتماده من قبل الإدارة في Supabase
            val isConfirmed = isUserConfirmedByAdmin(userObj)
            if (!isConfirmed) {
                return AuthResult.RequiresEmailVerification(
                    email = userEmail,
                    message = "الحساب ما زال قيد الانتظار ولم يتم تأكيده من قبل الإدارة في Supabase بعد."
                )
            }

            val session = SupabaseUserSession(
                userId = userId,
                email = userEmail,
                name = name,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            )
            saveSession(session)
            AuthResult.Success(session)
        } catch (e: Exception) {
            AuthResult.Error("فشل تحليل بيانات الجلسة: ${e.message}")
        }
    }

    /**
     * التحقق مما إذا كان الحساب قد تم اعتماده وتأكيده في Supabase (سواء بزر Confirm user أو عبر metadata)
     */
    fun isUserConfirmedByAdmin(userObj: JSONObject?): Boolean {
        if (userObj == null) return false
        val userMetadata = userObj.optJSONObject("user_metadata")
        val appMetadata = userObj.optJSONObject("app_metadata")

        // 1. فحص حقل الموافقة في user_metadata أو app_metadata إذا قام المدير بتعديلها من Supabase Users
        val isApprovedMeta = userMetadata?.optBoolean("is_approved", false) == true ||
                userMetadata?.optBoolean("approved", false) == true ||
                appMetadata?.optBoolean("is_approved", false) == true ||
                appMetadata?.optBoolean("approved", false) == true

        val status = (userMetadata?.optString("status") ?: "").lowercase()
        val isStatusApproved = status == "active" || status == "approved" || status == "confirmed"

        if (isApprovedMeta || isStatusApproved) {
            return true
        }

        // 2. فحص تاريخ تأكيد البريد الإلكتروني في Supabase
        val emailConfirmedAt = userObj.optString("email_confirmed_at", "")
        val confirmedAt = userObj.optString("confirmed_at", "")
        val effectiveConfirmedAt = emailConfirmedAt.ifBlank { confirmedAt }

        if (effectiveConfirmedAt.isBlank()) {
            return false
        }

        // إذا كان الحساب قد تم إنشاؤه مع وسم is_approved = false
        if (userMetadata != null && userMetadata.has("is_approved") && !userMetadata.optBoolean("is_approved", false)) {
            val createdAt = userObj.optString("created_at", "")
            val createdTime = parseIsoTime(createdAt)
            val confirmedTime = parseIsoTime(effectiveConfirmedAt)
            // إذا كان تاريخ التأكيد لاحقاً لتاريخ الإنشاء بأكثر من 5 ثوانٍ، فهذا تأكيد يدوي من المدير عبر زر Confirm user في Supabase
            if (createdTime > 0 && confirmedTime > 0 && (confirmedTime - createdTime) > 5000L) {
                return true
            }
            return false
        }

        return true
    }

    private fun parseIsoTime(isoString: String): Long {
        if (isoString.isBlank()) return 0L
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoString).toEpochMilli()
            } else {
                val cleanStr = isoString.substringBefore(".").substringBefore("Z")
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(cleanStr)?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun parseErrorMessage(responseBody: String): String {
        return try {
            val obj = JSONObject(responseBody)
            val msg = obj.optString("error_description", "")
                .ifBlank { obj.optString("msg", "") }
                .ifBlank { obj.optString("message", "") }

            when {
                msg.contains("Invalid login credentials", ignoreCase = true) ->
                    "البريد الإلكتروني أو كلمة المرور غير صحيحة."
                msg.contains("Email not confirmed", ignoreCase = true) ->
                    "الحساب ما زال بانتظار التفعيل والاعتماد من قبل الإدارة. يرجى التواصل مع الدعم."
                msg.contains("User already registered", ignoreCase = true) ->
                    "هذا البريد الإلكتروني مسجل بالفعل. يمكنك تسجيل الدخول أو فحص حالة التفعيل."
                msg.contains("Password should be at least", ignoreCase = true) ->
                    "كلمة المرور يجب أن تتكون من 6 أحرف على الأقل."
                msg.contains("Token has expired or is invalid", ignoreCase = true) ->
                    "رمز التحقق غير صحيح أو انتهت صلاحيته."
                msg.isNotBlank() -> msg
                else -> "حدث خطأ أثناء المصادقة، يرجى المحاولة مرة أخرى."
            }
        } catch (e: Exception) {
            "حدث خطأ أثناء الاتصال بالخادم، يرجى المحاولة لاحقاً."
        }
    }

    /**
     * حفظ بيانات الحساب قيد التفعيل والانتظار
     */
    fun savePendingAccount(name: String, email: String, password: String) {
        prefs.edit()
            .putString("pending_account_name", name.trim())
            .putString("pending_account_email", email.trim())
            .putString("pending_account_password", password)
            .putLong("pending_account_time", System.currentTimeMillis())
            .apply()
    }

    fun getPendingAccount(): PendingAccountData? {
        val email = prefs.getString("pending_account_email", null) ?: return null
        val name = prefs.getString("pending_account_name", "") ?: ""
        val password = prefs.getString("pending_account_password", "") ?: ""
        val time = prefs.getLong("pending_account_time", 0L)
        return PendingAccountData(name = name, email = email, password = password, registeredAt = time)
    }

    fun clearPendingAccount() {
        prefs.edit()
            .remove("pending_account_name")
            .remove("pending_account_email")
            .remove("pending_account_password")
            .remove("pending_account_time")
            .apply()
    }

    companion object {
        private const val PREF_SESSION_KEY = "key_supabase_session"
        private const val PREF_HAS_LOGGED_IN_BEFORE = "key_has_logged_in_before"

        @Volatile
        private var INSTANCE: SupabaseAuthManager? = null

        fun getInstance(context: Context): SupabaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
