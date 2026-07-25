package com.family.farecompare.data.history

import com.family.farecompare.data.local.RecentSearchDao
import com.family.farecompare.data.local.RecentSearchEntity
import com.family.farecompare.domain.history.SearchHistoryRepository
import com.family.farecompare.domain.model.RecentSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao
) : SearchHistoryRepository {

    override fun observeRecent(): Flow<List<RecentSearch>> = recentSearchDao.observeRecent().map { entities ->
        entities.map { RecentSearch(it.id, it.pickupAddress, it.destinationAddress, it.timestampMillis) }
    }

    override suspend fun recordSearch(pickupAddress: String, destinationAddress: String) {
        recentSearchDao.insert(
            RecentSearchEntity(
                pickupAddress = pickupAddress,
                destinationAddress = destinationAddress,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearHistory() {
        recentSearchDao.clearAll()
    }
}
