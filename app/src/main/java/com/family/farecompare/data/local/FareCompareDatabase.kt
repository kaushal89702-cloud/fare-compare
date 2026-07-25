package com.family.farecompare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecentSearchEntity::class], version = 1, exportSchema = false)
abstract class FareCompareDatabase : RoomDatabase() {
    abstract fun recentSearchDao(): RecentSearchDao
}
