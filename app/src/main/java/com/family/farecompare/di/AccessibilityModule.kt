package com.family.farecompare.di

import com.family.farecompare.data.accessibility.AccessibilityStatusCheckerImpl
import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccessibilityModule {

    @Binds
    abstract fun bindAccessibilityStatusChecker(
        impl: AccessibilityStatusCheckerImpl
    ): AccessibilityStatusChecker
}
