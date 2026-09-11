package com.pharmacy.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pharmacy.app.data.CartItem
import com.pharmacy.app.data.MedicineEntity
import com.pharmacy.app.data.PharmacyDatabase
import com.pharmacy.app.data.PharmacyRepository
import com.pharmacy.app.data.SaleRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * مدير حالة التطبيق الرئيسي (Main ViewModel)
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PharmacyRepository

    // حفظ واسترجاع اختيار الثيم (الوضع الداكن / الفاتح) محلياً
    private val prefs = application.getSharedPreferences("pharmacy_app_prefs", Context.MODE_PRIVATE)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("pref_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val nextMode = !_isDarkTheme.value
        _isDarkTheme.value = nextMode
        prefs.edit().putBoolean("pref_dark_theme", nextMode).apply()
    }

    init {
        val database = PharmacyDatabase.getDatabase(application)
        repository = PharmacyRepository(database.pharmacyDao())
        // ملء بيانات تجريبية عند التشغيل الأول
        viewModelScope.launch {
            repository.prepopulateSampleDataIfEmpty()
        }
    }

    // --- حالة البحث والفلترة ---
    val searchQuery = MutableStateFlow("")

    val allMedicines: StateFlow<List<MedicineEntity>> = repository.allMedicines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val searchResults: StateFlow<List<MedicineEntity>> = searchQuery
        .debounce(250L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allMedicines
            } else {
                repository.searchMedicines(query)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // --- التنبيهات (صلاحية ونواقص) ---
    private val _expiryAlertDays = MutableStateFlow(prefs.getInt("pref_expiry_alert_days", 30))
    val expiryAlertDays: StateFlow<Int> = _expiryAlertDays.asStateFlow()

    fun updateExpiryAlertDays(days: Int) {
        val validDays = days.coerceAtLeast(1)
        _expiryAlertDays.value = validDays
        prefs.edit().putInt("pref_expiry_alert_days", validDays).apply()
        _uiMessage.value = "تم ضبط مدة تنبيه انتهاء الصلاحية على $validDays يوماً"
    }

    // استرجاع وحفظ معرفات التنبيهات التي تم تأكيد توريدها في SharedPreferences
    private fun loadDismissedLowStockIds(): Set<Long> {
        val raw = prefs.getStringSet("pref_dismissed_low_stock_ids", emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private val _dismissedLowStockIds = MutableStateFlow<Set<Long>>(loadDismissedLowStockIds())
    val dismissedLowStockIds: StateFlow<Set<Long>> = _dismissedLowStockIds.asStateFlow()

    fun dismissLowStockAlert(medicineId: Long) {
        val updated = _dismissedLowStockIds.value + medicineId
        _dismissedLowStockIds.value = updated
        prefs.edit().putStringSet("pref_dismissed_low_stock_ids", updated.map { it.toString() }.toSet()).apply()
        _uiMessage.value = "تم تحديد الدواء كتم التوريد وحذف التنبيه بنجاح"
    }

    private fun undismissLowStockAlert(medicineId: Long) {
        if (medicineId in _dismissedLowStockIds.value) {
            val updated = _dismissedLowStockIds.value - medicineId
            _dismissedLowStockIds.value = updated
            prefs.edit().putStringSet("pref_dismissed_low_stock_ids", updated.map { it.toString() }.toSet()).apply()
        }
    }

    val lowStockMedicines: StateFlow<List<MedicineEntity>> = combine(
        repository.lowStockMedicines,
        _dismissedLowStockIds
    ) { list, dismissed ->
        list.filter { it.id !in dismissed }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val expiringSoonMedicines: StateFlow<List<MedicineEntity>> = _expiryAlertDays
        .flatMapLatest { days ->
            repository.getExpiringSoonMedicines(days)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val expiredMedicines: StateFlow<List<MedicineEntity>> = repository.getExpiredMedicines().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val totalAlertsCount: StateFlow<Int> = combine(lowStockMedicines, expiredMedicines, expiringSoonMedicines) { low, exp, soon ->
        low.size + exp.size + soon.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )

    // --- المبيعات والتقارير المالية ---
    val allSales: StateFlow<List<SaleRecordEntity>> = repository.allSales.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // تجميع المبيعات إلى فواتير: كل فاتورة تحتوي على رقم الفاتورة، الوقت، الإجمالي، عدد الأدوية
    val invoices: StateFlow<List<com.pharmacy.app.data.InvoiceSummary>> = allSales.map { sales ->
        sales.groupBy { it.invoiceId }
            .map { (invId, items) ->
                val first = items.first()
                com.pharmacy.app.data.InvoiceSummary(
                    invoiceId = invId,
                    timestamp = first.timestamp,
                    totalAmount = items.sumOf { it.totalSellPrice },
                    totalProfit = items.sumOf { it.totalProfit },
                    itemsCount = items.size, // عدد الأدوية المختلفة في الفاتورة
                    totalUnitsSold = items.sumOf { it.quantitySold },
                    items = items
                )
            }
            .sortedByDescending { it.timestamp }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val totalRevenue: StateFlow<Double> = repository.totalRevenue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    val totalProfit: StateFlow<Double> = repository.totalProfit.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    val totalItemsSold: StateFlow<Int> = repository.totalItemsSold.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )

    // --- سلة المبيعات الحالية (POS Cart) ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Double> = _cart.map { items ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartProfit: StateFlow<Double> = _cart.map { items ->
        items.sumOf { it.profit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // رسائل التنبيه والواجهة للمستخدم
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // باركود جاهز للتعبئة في حال مسح باركود غير موجود لإضافته فوراً
    private val _pendingBarcodeForAdd = MutableStateFlow<String?>(null)
    val pendingBarcodeForAdd: StateFlow<String?> = _pendingBarcodeForAdd.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun clearPendingBarcode() {
        _pendingBarcodeForAdd.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    // --- عمليات سلة نقطة البيع (POS) ---
    fun addToCart(medicine: MedicineEntity, qty: Int = 1) {
        if (medicine.quantity <= 0) {
            _uiMessage.value = "عذراً، هذا الدواء نفد من المخزون تماماً!"
            return
        }

        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.medicine.id == medicine.id }

        if (existingIndex >= 0) {
            val existingItem = currentList[existingIndex]
            val newQty = existingItem.quantity + qty
            if (newQty > medicine.quantity) {
                _uiMessage.value = "الكمية المطلوبة تتجاوز المخزون المتاح (${medicine.quantity})"
                return
            }
            currentList[existingIndex] = existingItem.copy(quantity = newQty)
        } else {
            if (qty > medicine.quantity) {
                _uiMessage.value = "الكمية المطلوبة تتجاوز المخزون المتاح (${medicine.quantity})"
                return
            }
            currentList.add(CartItem(medicine = medicine, quantity = qty))
        }
        _cart.value = currentList
        _uiMessage.value = "تمت إضافة ${medicine.name} للسلة"
    }

    fun updateCartQuantity(medicineId: Long, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(medicineId)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.medicine.id == medicineId }
        if (index >= 0) {
            val item = currentList[index]
            if (newQty > item.medicine.quantity) {
                _uiMessage.value = "الكمية المطلوبة تتجاوز المخزون (${item.medicine.quantity})"
                return
            }
            currentList[index] = item.copy(quantity = newQty)
            _cart.value = currentList
        }
    }

    fun removeFromCart(medicineId: Long) {
        _cart.value = _cart.value.filterNot { it.medicine.id == medicineId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun checkoutCart() {
        viewModelScope.launch {
            val items = _cart.value
            if (items.isEmpty()) return@launch

            val success = repository.checkoutCart(items)
            if (success) {
                _cart.value = emptyList()
                _uiMessage.value = "تمت عملية البيع بنجاح وخصم الكميات من المخزون!"
            } else {
                _uiMessage.value = "فشلت عملية البيع، يرجى مراجعة كميات المخزون المتوفرة"
            }
        }
    }

    // --- معالجة مسح الباركود ---
    fun onBarcodeScanned(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return

        viewModelScope.launch {
            val found = repository.getMedicineByBarcode(cleanBarcode)
            if (found != null) {
                addToCart(found, 1)
            } else {
                _pendingBarcodeForAdd.value = cleanBarcode
                _uiMessage.value = "الباركود غير مسجل: $cleanBarcode (يمكنك إضافته كدواء جديد)"
            }
        }
    }

    // --- إدارة الأدوية في المخزون ---
    fun addOrAccumulateMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            val (resultMed, wasAccumulated) = repository.addOrAccumulateMedicine(medicine)
            if (resultMed.quantity > resultMed.minStockAlert) {
                undismissLowStockAlert(resultMed.id)
            }
            if (wasAccumulated) {
                _uiMessage.value = "تمت إضافة الكمية (${medicine.quantity}) إلى الدواء '${resultMed.name}'. إجمالي المخزون الحالي: ${resultMed.quantity}"
            } else {
                _uiMessage.value = "تمت إضافة الدواء بنجاح للمخزون: ${resultMed.name}"
            }
        }
    }

    fun saveMedicine(medicine: MedicineEntity) {
        addOrAccumulateMedicine(medicine)
    }

    fun updateMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.updateMedicine(medicine)
            if (medicine.quantity > medicine.minStockAlert) {
                undismissLowStockAlert(medicine.id)
            }
            _uiMessage.value = "تم تحديث الدواء بنجاح: ${medicine.name}"
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            // إزالته أيضاً من السلة إذا وجد
            removeFromCart(medicine.id)
            _uiMessage.value = "تم حذف الدواء من المخزون: ${medicine.name}"
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            val success = repository.deleteInvoice(invoiceId)
            if (success) {
                _uiMessage.value = "تم حذف الفاتورة $invoiceId بنجاح"
            } else {
                _uiMessage.value = "تعذر حذف الفاتورة $invoiceId"
            }
        }
    }

    fun adjustStock(medicineId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustStock(medicineId, delta)
        }
    }
}
