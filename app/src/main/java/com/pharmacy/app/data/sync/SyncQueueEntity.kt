package com.pharmacy.app.data.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * أنواع العمليات المطلوب مزامنتها مع Supabase
 */
enum class SyncOperationType {
    UPSERT_MEDICINE,
    DELETE_MEDICINE,
    INSERT_SALE,
    DELETE_INVOICE
}

/**
 * عنصر في قائمة انتظار المزامنة المحلية (Room Sync Queue)
 * يحفظ نوع العملية، معرف السجل، والبيانات بصيغة JSON
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String, // SyncOperationType.name
    val entityType: String,    // "medicine" or "sale"
    val localId: Long,
    val payloadJson: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    suspend fun getAllPending(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    fun getAllPendingFlow(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
