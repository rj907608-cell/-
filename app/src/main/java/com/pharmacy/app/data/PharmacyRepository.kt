package com.pharmacy.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * عنصر في سلة المبيعات لنقطة البيع (POS Cart Item)
 * يتم حساب إجمالي الفاتورة بضرب الكمية في سعر البيع دائماً
 */
data class CartItem(
    val medicine: MedicineEntity,
    val quantity: Int
) {
    // التأكيد التام على أن الحساب يتم دائماً بضرب سعر البيع
    val effectiveSellPrice: Double get() = if (medicine.sellPrice > 0.0) medicine.sellPrice else medicine.buyPrice
    val subtotal: Double get() = effectiveSellPrice * quantity
    val profit: Double get() = (effectiveSellPrice - medicine.buyPrice) * quantity
}

/**
 * مستودع إدارة بيانات الصيدلية (Pharmacy Repository)
 */
class PharmacyRepository(
    private val dao: PharmacyDao,
    private val syncEngine: com.pharmacy.app.data.sync.PharmacySyncEngine? = null
) {

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

    fun getExpiringSoonMedicines(daysAhead: Int = 30): Flow<List<MedicineEntity>> {
        val targetTimestamp = System.currentTimeMillis() + (daysAhead.toLong() * 24 * 60 * 60 * 1000)
        return dao.getExpiringSoonMedicines(targetTimestamp)
    }

    fun getExpiredMedicines(): Flow<List<MedicineEntity>> {
        return dao.getExpiredMedicines(System.currentTimeMillis())
    }

    suspend fun saveMedicine(medicine: MedicineEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = dao.insertMedicine(medicine)
        val finalEntity = if (medicine.id <= 0) medicine.copy(id = insertedId) else medicine
        syncEngine?.enqueueOperation(
            operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
            entityType = "medicine",
            localId = insertedId,
            payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(finalEntity)
        )
        insertedId
    }

    /**
     * إضافة دواء جديد أو تراكُم كميته ودفوعاته إن كان مسجلاً مسبقاً في المخزون
     */
    suspend fun addOrAccumulateMedicine(newMed: MedicineEntity): Pair<MedicineEntity, Boolean> = withContext(Dispatchers.IO) {
        // التحقق أولاً برقم الباركود إن كان غير فارغ
        var existing: MedicineEntity? = if (newMed.barcode.isNotBlank()) {
            dao.getMedicineByBarcode(newMed.barcode.trim())
        } else null

        // إذا لم يُعثر عليه بالباركود، نبحث بالاسم المطابق إن كان الاسم غير فارغ
        if (existing == null && newMed.name.isNotBlank()) {
            existing = dao.getMedicineByName(newMed.name.trim())
        }

        if (existing != null) {
            // الدواء موجود مسبقاً في المخزون: تراكُم الكميات
            val existingBatches = existing.getBatches().toMutableList()
            val originalQty = existing.quantity
            val addedQty = newMed.quantity
            val newTotalQty = originalQty + addedQty

            // التأكد من تسجيل المخزون السابق كدفعة أولى إن لم تكن مسجلة
            if (existingBatches.isEmpty() && originalQty > 0) {
                existingBatches.add(
                    MedicineBatch(
                        batchNumber = 1,
                        quantity = originalQty,
                        buyPrice = existing.buyPrice,
                        sellPrice = existing.sellPrice,
                        dateAdded = existing.createdAt
                    )
                )
            }

            // إذا كان السعر الجديد مختلفاً عن السعر القديم، نحتفظ بالكمية والسعر لكل دفعة بشكل منفصل
            val lastBatch = existingBatches.lastOrNull()
            val isSamePrice = lastBatch != null &&
                    lastBatch.sellPrice == newMed.sellPrice &&
                    lastBatch.buyPrice == newMed.buyPrice

            if (isSamePrice && lastBatch != null) {
                // السعر مطابق: نزيد كمية نفس الدفعة
                existingBatches[existingBatches.lastIndex] = lastBatch.copy(
                    quantity = lastBatch.quantity + addedQty
                )
            } else {
                // السعر مختلف أو دفعة جديدة: ننشئ دفعة منفصلة جديدة
                val nextBatchNumber = (existingBatches.maxOfOrNull { it.batchNumber } ?: 0) + 1
                existingBatches.add(
                    MedicineBatch(
                        batchNumber = nextBatchNumber,
                        quantity = addedQty,
                        buyPrice = newMed.buyPrice,
                        sellPrice = newMed.sellPrice,
                        dateAdded = System.currentTimeMillis()
                    )
                )
            }

            val updatedEntity = existing.copy(
                quantity = newTotalQty,
                sellPrice = if (newMed.sellPrice > 0) newMed.sellPrice else existing.sellPrice,
                buyPrice = if (newMed.buyPrice > 0) newMed.buyPrice else existing.buyPrice,
                category = if (newMed.category.isNotBlank()) newMed.category else existing.category,
                location = if (newMed.location.isNotBlank()) newMed.location else existing.location,
                expiryDate = if (newMed.expiryDate > 0L) newMed.expiryDate else existing.expiryDate,
                batchesJson = BatchConverter.toJson(existingBatches)
            )

            dao.updateMedicine(updatedEntity)
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
                entityType = "medicine",
                localId = updatedEntity.id,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(updatedEntity)
            )
            Pair(updatedEntity, true) // تم التراكم
        } else {
            // دواء جديد كلياً
            val initialBatches = if (newMed.quantity > 0) {
                listOf(
                    MedicineBatch(
                        batchNumber = 1,
                        quantity = newMed.quantity,
                        buyPrice = newMed.buyPrice,
                        sellPrice = newMed.sellPrice,
                        dateAdded = System.currentTimeMillis()
                    )
                )
            } else emptyList()

            val entityToInsert = newMed.copy(
                batchesJson = BatchConverter.toJson(initialBatches)
            )
            val newId = dao.insertMedicine(entityToInsert)
            val insertedEntity = entityToInsert.copy(id = newId)
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
                entityType = "medicine",
                localId = newId,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(insertedEntity)
            )
            Pair(insertedEntity, false) // إضافة جديدة
        }
    }

    suspend fun updateMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        dao.updateMedicine(medicine)
        syncEngine?.enqueueOperation(
            operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
            entityType = "medicine",
            localId = medicine.id,
            payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(medicine)
        )
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        dao.deleteMedicine(medicine)
        syncEngine?.enqueueOperation(
            operationType = com.pharmacy.app.data.sync.SyncOperationType.DELETE_MEDICINE,
            entityType = "medicine",
            localId = medicine.id,
            payloadJson = "{}"
        )
    }

    suspend fun adjustStock(medicineId: Long, delta: Int): Boolean = withContext(Dispatchers.IO) {
        val medicine = dao.getMedicineById(medicineId) ?: return@withContext false
        val newQty = medicine.quantity + delta
        if (newQty < 0) return@withContext false

        val batches = medicine.getBatches().toMutableList()
        if (delta > 0) {
            if (batches.isNotEmpty()) {
                val last = batches.last()
                batches[batches.lastIndex] = last.copy(quantity = last.quantity + delta)
            } else {
                batches.add(
                    MedicineBatch(
                        batchNumber = 1,
                        quantity = newQty,
                        buyPrice = medicine.buyPrice,
                        sellPrice = medicine.sellPrice
                    )
                )
            }
        } else if (delta < 0) {
            var toDeduct = -delta
            for (i in batches.indices) {
                if (toDeduct <= 0) break
                val b = batches[i]
                if (b.quantity <= toDeduct) {
                    toDeduct -= b.quantity
                    batches[i] = b.copy(quantity = 0)
                } else {
                    batches[i] = b.copy(quantity = b.quantity - toDeduct)
                    toDeduct = 0
                }
            }
        }

        val updated = medicine.copy(
            quantity = newQty,
            batchesJson = BatchConverter.toJson(batches)
        )
        dao.updateMedicine(updated)
        syncEngine?.enqueueOperation(
            operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
            entityType = "medicine",
            localId = updated.id,
            payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(updated)
        )
        true
    }

    /**
     * توليد رقم فاتورة متسلسل ومنتظم (INV-0001, INV-0002, ...)
     */
    suspend fun generateSequentialInvoiceId(): String = withContext(Dispatchers.IO) {
        val existingIds = dao.getAllInvoiceIds()
        var maxNumber = 0
        for (id in existingIds) {
            val num = id.filter { it.isDigit() }.toIntOrNull()
            if (num != null && num > maxNumber) {
                maxNumber = num
            }
        }
        val nextSeq = maxNumber + 1
        String.format(java.util.Locale.US, "INV-%04d", nextSeq)
    }

    /**
     * إتمام عملية بيع سلة كاملة وتحديث المخزون وتسجيل القيود المالية
     * الحساب يتم دائماً بضرب الكمية في سعر البيع
     */
    suspend fun checkoutCart(items: List<CartItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext false
        val invoiceId = generateSequentialInvoiceId()
        val salesList = mutableListOf<SaleRecordEntity>()

        for (item in items) {
            val medicine = dao.getMedicineById(item.medicine.id) ?: return@withContext false
            if (medicine.quantity < item.quantity) {
                return@withContext false
            }

            val newQty = medicine.quantity - item.quantity
            val batches = medicine.getBatches().toMutableList()
            var toDeduct = item.quantity
            for (i in batches.indices) {
                if (toDeduct <= 0) break
                val b = batches[i]
                if (b.quantity <= toDeduct) {
                    toDeduct -= b.quantity
                    batches[i] = b.copy(quantity = 0)
                } else {
                    batches[i] = b.copy(quantity = b.quantity - toDeduct)
                    toDeduct = 0
                }
            }

            val updatedMedicine = medicine.copy(
                quantity = newQty,
                batchesJson = BatchConverter.toJson(batches)
            )
            dao.updateMedicine(updatedMedicine)
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
                entityType = "medicine",
                localId = updatedMedicine.id,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(updatedMedicine)
            )

            val unitSell = item.effectiveSellPrice
            salesList.add(
                SaleRecordEntity(
                    invoiceId = invoiceId,
                    medicineId = item.medicine.id,
                    medicineName = item.medicine.name,
                    barcode = item.medicine.barcode,
                    quantitySold = item.quantity,
                    unitCostPrice = item.medicine.buyPrice,
                    unitSellPrice = unitSell,
                    totalSellPrice = item.subtotal, // الكمية × سعر البيع
                    totalProfit = item.profit,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        val insertedIds = dao.insertSales(salesList)
        // إضافة المبيعات لقائمة المزامنة
        for (i in salesList.indices) {
            val sale = salesList[i]
            val saleId = insertedIds.getOrNull(i) ?: 0L
            val finalSale = sale.copy(id = saleId)
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.INSERT_SALE,
                entityType = "sale",
                localId = saleId,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.saleToJson(finalSale)
            )
        }
        true
    }

    /**
     * بيع سريع لدواء مفرد بحساب ضرب سعر البيع
     */
    suspend fun quickSell(medicine: MedicineEntity, quantity: Int = 1): Boolean = withContext(Dispatchers.IO) {
        if (medicine.quantity < quantity) return@withContext false

        val affected = dao.deductStock(medicine.id, quantity)
        if (affected > 0) {
            val invoiceId = generateSequentialInvoiceId()
            val unitSell = if (medicine.sellPrice > 0.0) medicine.sellPrice else medicine.buyPrice
            val sale = SaleRecordEntity(
                invoiceId = invoiceId,
                medicineId = medicine.id,
                medicineName = medicine.name,
                barcode = medicine.barcode,
                quantitySold = quantity,
                unitCostPrice = medicine.buyPrice,
                unitSellPrice = unitSell,
                totalSellPrice = unitSell * quantity, // الكمية × سعر البيع
                totalProfit = (unitSell - medicine.buyPrice) * quantity,
                timestamp = System.currentTimeMillis()
            )
            val insertedSaleId = dao.insertSale(sale)
            val finalSale = sale.copy(id = insertedSaleId)
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.INSERT_SALE,
                entityType = "sale",
                localId = insertedSaleId,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.saleToJson(finalSale)
            )

            // تحديث المخزون في السحابة أيضاً
            val updatedMed = dao.getMedicineById(medicine.id)
            if (updatedMed != null) {
                syncEngine?.enqueueOperation(
                    operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
                    entityType = "medicine",
                    localId = updatedMed.id,
                    payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(updatedMed)
                )
            }
            true
        } else {
            false
        }
    }

    /**
     * حذف فاتورة كاملة بجميع أدوية البيع المسجلة تحتها واسترجاع الكميات للمخزون
     */
    suspend fun deleteInvoice(invoiceId: String): Boolean = withContext(Dispatchers.IO) {
        val sales = dao.getSalesByInvoiceId(invoiceId)
        // استرجاع كميات الأدوية المباعة إلى المخزون تلقائياً
        for (sale in sales) {
            val med = dao.getMedicineById(sale.medicineId)
            if (med != null) {
                val newQty = med.quantity + sale.quantitySold
                val batches = med.getBatches().toMutableList()
                if (batches.isNotEmpty()) {
                    val lastBatch = batches.last()
                    batches[batches.lastIndex] = lastBatch.copy(quantity = lastBatch.quantity + sale.quantitySold)
                } else {
                    batches.add(
                        MedicineBatch(
                            batchNumber = 1,
                            quantity = sale.quantitySold,
                            buyPrice = sale.unitCostPrice,
                            sellPrice = sale.unitSellPrice,
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }
                val updatedMed = med.copy(
                    quantity = newQty,
                    batchesJson = BatchConverter.toJson(batches)
                )
                dao.updateMedicine(updatedMed)
                syncEngine?.enqueueOperation(
                    operationType = com.pharmacy.app.data.sync.SyncOperationType.UPSERT_MEDICINE,
                    entityType = "medicine",
                    localId = updatedMed.id,
                    payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.medicineToJson(updatedMed)
                )
            }
        }
        val count = dao.deleteSalesByInvoiceId(invoiceId)
        if (count > 0) {
            syncEngine?.enqueueOperation(
                operationType = com.pharmacy.app.data.sync.SyncOperationType.DELETE_INVOICE,
                entityType = "sale",
                localId = 0L,
                payloadJson = com.pharmacy.app.data.sync.SyncJsonHelper.invoiceDeleteToJson(invoiceId)
            )
            true
        } else {
            false
        }
    }

    /**
     * تهيئة بيانات أولية نموذجية للمخزون إذا كانت قاعدة البيانات فارغة لأول مرة
     */
    suspend fun prepopulateSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // قاعدة البيانات تبدأ فارغة تماماً بدون أدوية مدخلة مسبقاً
    }
}
