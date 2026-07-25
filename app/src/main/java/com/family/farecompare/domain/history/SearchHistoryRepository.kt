package com.family.farecompare.domain.history

import com.family.farecompare.domain.model.RecentSearch
import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observeRecent(): Flow<List<RecentSearch>>
    suspend fun recordSearch(pickupAddress: String, destinationAddress: String)
    suspend fun clearHistory()
}
