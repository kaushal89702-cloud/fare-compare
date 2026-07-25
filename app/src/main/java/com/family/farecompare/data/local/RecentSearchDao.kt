package com.family.farecompare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val MAX_RECENT_SEARCHES = 20

@Dao
interface RecentSearchDao {

    @Insert
    suspend fun insert(entity: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY timestampMillis DESC LIMIT $MAX_RECENT_SEARCHES")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
