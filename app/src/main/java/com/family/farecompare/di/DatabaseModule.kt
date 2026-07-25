package com.family.farecompare.di

import android.content.Context
import androidx.room.Room
import com.family.farecompare.data.history.SearchHistoryRepositoryImpl
import com.family.farecompare.data.local.FareCompareDatabase
import com.family.farecompare.data.local.RecentSearchDao
import com.family.farecompare.domain.history.SearchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "farecompare.db"

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): FareCompareDatabase =
            Room.databaseBuilder(context, FareCompareDatabase::class.java, DATABASE_NAME).build()

        @Provides
        fun provideRecentSearchDao(database: FareCompareDatabase): RecentSearchDao = database.recentSearchDao()
    }
}
