package com.hatuka.swipeclean.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hatuka.swipeclean.core.review.DecisionState
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionDao {
    @Upsert
    suspend fun upsert(entity: DecisionEntity)

    @Query("DELETE FROM decision WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("SELECT mediaId FROM decision")
    suspend fun allDecidedIds(): List<Long>

    @Query("SELECT * FROM decision WHERE mediaId = :mediaId")
    suspend fun get(mediaId: Long): DecisionEntity?

    @Query("SELECT * FROM decision WHERE state = :state ORDER BY decidedAt DESC")
    fun observeByState(state: DecisionState): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decision WHERE state = :state")
    suspend fun byState(state: DecisionState): List<DecisionEntity>

    @Query("SELECT COUNT(*) AS count, COALESCE(SUM(sizeBytes), 0) AS bytes FROM decision WHERE state = :state")
    fun observeSummary(state: DecisionState): Flow<BinSummary>

    @Query("UPDATE decision SET state = :state, targetPath = :targetPath, decidedAt = :now WHERE mediaId IN (:ids)")
    suspend fun setState(ids: List<Long>, state: DecisionState, targetPath: String?, now: Long)
}

@Dao
abstract class StatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(entity: DailyStatsEntity)

    @Query(
        """UPDATE daily_stats SET
            reviewed = MAX(reviewed + :reviewed, 0),
            marked_delete = MAX(marked_delete + :markedDelete, 0),
            deleted = MAX(deleted + :deleted, 0),
            bytes_freed = MAX(bytes_freed + :bytesFreed, 0)
           WHERE epochDay = :day""",
    )
    abstract suspend fun addTo(day: Long, reviewed: Int, markedDelete: Int, deleted: Int, bytesFreed: Long)

    @Transaction
    open suspend fun add(day: Long, reviewed: Int = 0, markedDelete: Int = 0, deleted: Int = 0, bytesFreed: Long = 0) {
        insertIgnore(DailyStatsEntity(day))
        addTo(day, reviewed, markedDelete, deleted, bytesFreed)
    }

    @Query("SELECT * FROM daily_stats WHERE epochDay = :day")
    abstract fun observeDay(day: Long): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats ORDER BY epochDay")
    abstract fun observeAll(): Flow<List<DailyStatsEntity>>
}
