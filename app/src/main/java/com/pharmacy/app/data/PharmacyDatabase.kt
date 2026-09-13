package com.pharmacy.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * دواء في المخزون (Medicine in Inventory)
 */
@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val barcode: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val quantity: Int = 0,
    val minStockAlert: Int = 0,
    val expiryDate: Long = 0L, // 0L يعني غير محدد
    val category: String = "أدوية عامة",
    val location: String = "",
    val notes: String = "",
    val batchesJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * استرجاع قائمة الدفعات المسجلة للدواء مع تفاصيل الكمية والسعر لكل دفعة
     */
    fun getBatches(): List<MedicineBatch> {
        val parsed = BatchConverter.fromJson(batchesJson)
        if (parsed.isNotEmpty()) return parsed
        return if (quantity > 0) {
            listOf(
                MedicineBatch(
                    batchNumber = 1,
                    quantity = quantity,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    dateAdded = createdAt
                )
            )
        } else emptyList()
    }
}

/**
 * سجل المبيعات (Sales Record)
 */
@Entity(tableName = "sales")
data class SaleRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: String,
    val medicineId: Long,
    val medicineName: String,
    val barcode: String,
    val quantitySold: Int,
    val unitCostPrice: Double,
    val unitSellPrice: Double,
    val totalSellPrice: Double,
    val totalProfit: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * أوامر التعامل مع قاعدة البيانات (Data Access Object)
 */
@Dao
interface PharmacyDao {

    // --- Medicines Operations ---
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE barcode = :barcode LIMIT 1")
    suspend fun getMedicineByBarcode(barcode: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE name = :name LIMIT 1")
    suspend fun getMedicineByName(name: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE barcode = :barcode LIMIT 1")
    fun getMedicineByBarcodeFlow(barcode: String): Flow<MedicineEntity?>

    @Query("SELECT * FROM medicines WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMedicines(query: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE quantity <= minStockAlert ORDER BY quantity ASC")
    fun getLowStockMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE expiryDate > 0 AND expiryDate <= :futureTimestamp ORDER BY expiryDate ASC")
    fun getExpiringSoonMedicines(futureTimestamp: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE expiryDate > 0 AND expiryDate < :currentTimestamp ORDER BY expiryDate ASC")
    fun getExpiredMedicines(currentTimestamp: Long): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET quantity = quantity - :quantity WHERE id = :id AND quantity >= :quantity")
    suspend fun deductStock(id: Long, quantity: Int): Int

    @Query("UPDATE medicines SET quantity = quantity + :quantity WHERE id = :id")
    suspend fun addStock(id: Long, quantity: Int): Int

    // --- Sales Operations ---
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleRecordEntity>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleRecordEntity>): List<Long>

    @Query("SELECT COALESCE(SUM(totalSellPrice), 0.0) FROM sales")
    fun getTotalRevenue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalProfit), 0.0) FROM sales")
    fun getTotalProfit(): Flow<Double>

    @Query("SELECT COALESCE(SUM(quantitySold), 0) FROM sales")
    fun getTotalItemsSold(): Flow<Int>

    @Query("SELECT * FROM sales WHERE invoiceId = :invoiceId")
    suspend fun getSalesByInvoiceId(invoiceId: String): List<SaleRecordEntity>

    @Query("SELECT invoiceId FROM sales")
    suspend fun getAllInvoiceIds(): List<String>

    @Query("DELETE FROM sales WHERE invoiceId = :invoiceId")
    suspend fun deleteSalesByInvoiceId(invoiceId: String): Int

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun getMedicineCount(): Int
}

/**
 * قاعدة بيانات الصيدلية الرئيسية (Room Database)
 */
@Database(
    entities = [MedicineEntity::class, SaleRecordEntity::class, com.pharmacy.app.data.sync.SyncQueueEntity::class],
    version = 3,
    exportSchema = false
)
abstract class PharmacyDatabase : RoomDatabase() {
    abstract fun pharmacyDao(): PharmacyDao
    abstract fun syncQueueDao(): com.pharmacy.app.data.sync.SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: PharmacyDatabase? = null

        fun getDatabase(context: Context): PharmacyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PharmacyDatabase::class.java,
                    "pharmacy_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
