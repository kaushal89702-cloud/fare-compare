package com.family.farecompare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pickupAddress: String,
    val destinationAddress: String,
    val timestampMillis: Long
)
