package com.pharmacy.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * عنصر في سلة المبيعات لنقطة البيع (POS Cart Item)
 */
data class CartItem(
    val medicine: MedicineEntity,
    val quantity: Int
) {
    val subtotal: Double get() = medicine.sellPrice * quantity
    val profit: Double get() = (medicine.sellPrice - medicine.buyPrice) * quantity
}

/**
 * مستودع إدارة بيانات الصيدلية (Pharmacy Repository)
 */
class PharmacyRepository(private val dao: PharmacyDao) {

    val allMedicines: Flow<List<MedicineEntity>> = dao.getAllMedicines()
    val lowStockMedicines: Flow<List<MedicineEntity>> = dao.getLowStockMedicines()
    val allSales: Flow<List<SaleRecordEntity>> = dao.getAllSales()
    val totalRevenue: Flow<Double> = dao.getTotalRevenue()
    val totalProfit: Flow<Double> = dao.getTotalProfit()
    val totalItemsSold: Flow<Int> = dao.getTotalItemsSold()

    fun searchMedicines(query: String): Flow<List<MedicineEntity>> {
        return dao.searchMedicines(query.trim())
    }

    suspend fun getMedicineByBarcode(barcode: String): MedicineEntity? = withContext(Dispatchers.IO) {
        dao.getMedicineByBarcode(barcode.trim())
    }

    suspend fun getMedicineById(id: Long): MedicineEntity? = withContext(Dispatchers.IO) {
        dao.getMedicineById(id)
    }

    fun getExpiringSoonMedicines(daysAhead: Int = 90): Flow<List<MedicineEntity>> {
        val targetTimestamp = System.currentTimeMillis() + (daysAhead.toLong() * 24 * 60 * 60 * 1000)
        return dao.getExpiringSoonMedicines(targetTimestamp)
    }

    fun getExpiredMedicines(): Flow<List<MedicineEntity>> {
        return dao.getExpiredMedicines(System.currentTimeMillis())
    }

    suspend fun saveMedicine(medicine: MedicineEntity): Long = withContext(Dispatchers.IO) {
        dao.insertMedicine(medicine)
    }

    suspend fun updateMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        dao.updateMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        dao.deleteMedicine(medicine)
    }

    suspend fun adjustStock(medicineId: Long, delta: Int): Boolean = withContext(Dispatchers.IO) {
        if (delta > 0) {
            dao.addStock(medicineId, delta) > 0
        } else if (delta < 0) {
            dao.deductStock(medicineId, -delta) > 0
        } else {
            true
        }
    }

    /**
     * إتمام عملية بيع سلة كاملة وتحديث المخزون وتسجيل القيود المالية
     */
    suspend fun checkoutCart(items: List<CartItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext false
        val invoiceId = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
        val salesList = mutableListOf<SaleRecordEntity>()

        for (item in items) {
            // خصم الكمية من المخزون
            val affectedRows = dao.deductStock(item.medicine.id, item.quantity)
            if (affectedRows <= 0) {
                // الكمية غير كافية في المخزون
                return@withContext false
            }

            salesList.add(
                SaleRecordEntity(
                    invoiceId = invoiceId,
                    medicineId = item.medicine.id,
                    medicineName = item.medicine.name,
                    barcode = item.medicine.barcode,
                    quantitySold = item.quantity,
                    unitCostPrice = item.medicine.buyPrice,
                    unitSellPrice = item.medicine.sellPrice,
                    totalSellPrice = item.subtotal,
                    totalProfit = item.profit,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        dao.insertSales(salesList)
        true
    }

    /**
     * بيع سريع لدواء مفرد
     */
    suspend fun quickSell(medicine: MedicineEntity, quantity: Int = 1): Boolean = withContext(Dispatchers.IO) {
        if (medicine.quantity < quantity) return@withContext false

        val affected = dao.deductStock(medicine.id, quantity)
        if (affected > 0) {
            val sale = SaleRecordEntity(
                invoiceId = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
                medicineId = medicine.id,
                medicineName = medicine.name,
                barcode = medicine.barcode,
                quantitySold = quantity,
                unitCostPrice = medicine.buyPrice,
                unitSellPrice = medicine.sellPrice,
                totalSellPrice = medicine.sellPrice * quantity,
                totalProfit = (medicine.sellPrice - medicine.buyPrice) * quantity,
                timestamp = System.currentTimeMillis()
            )
            dao.insertSale(sale)
            true
        } else {
            false
        }
    }

    /**
     * تهيئة بيانات أولية نموذجية للمخزون إذا كانت قاعدة البيانات فارغة لأول مرة
     */
    suspend fun prepopulateSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getMedicineCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val oneDay = 24L * 60 * 60 * 1000
            val sampleMedicines = listOf(
                MedicineEntity(
                    name = "بانادول إكسترا 500 ملغ (Panadol Extra)",
                    barcode = "6281001234567",
                    buyPrice = 12.50,
                    sellPrice = 18.00,
                    quantity = 35,
                    minStockAlert = 10,
                    expiryDate = now + (365L * oneDay), // صالح لسنة
                    category = "مسكنات وخافض حرارة",
                    location = "رف A-1"
                ),
                MedicineEntity(
                    name = "أوجمنتين 1 غرام (Augmentin 1g)",
                    barcode = "6281007654321",
                    buyPrice = 45.00,
                    sellPrice = 62.00,
                    quantity = 4, // منخفض المخزون للتنبيه
                    minStockAlert = 8,
                    expiryDate = now + (180L * oneDay),
                    category = "مضادات حيوية",
                    location = "رف B-2"
                ),
                MedicineEntity(
                    name = "أوميبريزول 20 ملغ (Omeprazole 20mg)",
                    barcode = "6281009876543",
                    buyPrice = 20.00,
                    sellPrice = 29.50,
                    quantity = 22,
                    minStockAlert = 5,
                    expiryDate = now + (25L * oneDay), // قارب على الانتهاء (أقل من شهر)
                    category = "أدوية المعدة والجهاز الهضمي",
                    location = "رف C-3"
                ),
                MedicineEntity(
                    name = "فيتامين سي فوار 1000 ملغ (Vitamin C)",
                    barcode = "6281003456789",
                    buyPrice = 15.00,
                    sellPrice = 22.00,
                    quantity = 18,
                    minStockAlert = 5,
                    expiryDate = now + (400L * oneDay),
                    category = "مكملات غذائية وفيتامينات",
                    location = "رف A-4"
                ),
                MedicineEntity(
                    name = "كلاريتين 10 ملغ (Claritin 10mg)",
                    barcode = "6281004567890",
                    buyPrice = 25.00,
                    sellPrice = 36.00,
                    quantity = 2, // وشك النفاد
                    minStockAlert = 6,
                    expiryDate = now - (5L * oneDay), // منتهي الصلاحية لاختبار التنبيهات
                    category = "حساسية ومضادات الهيستامين",
                    location = "رف B-1"
                ),
                MedicineEntity(
                    name = "بروفين 400 ملغ (Brufen 400mg)",
                    barcode = "6281005678901",
                    buyPrice = 14.00,
                    sellPrice = 20.00,
                    quantity = 40,
                    minStockAlert = 10,
                    expiryDate = now + (200L * oneDay),
                    category = "مسكنات ومضادات التهاب",
                    location = "رف A-2"
                )
            )

            for (med in sampleMedicines) {
                dao.insertMedicine(med)
            }
        }
    }
}
