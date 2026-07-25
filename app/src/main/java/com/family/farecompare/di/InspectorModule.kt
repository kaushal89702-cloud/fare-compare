package com.family.farecompare.di

import com.family.farecompare.data.automation.SelfAppReturnerImpl
import com.family.farecompare.data.inspector.UiInspectorRepositoryImpl
import com.family.farecompare.data.inspector.UiTreeDumperImpl
import com.family.farecompare.data.inspector.UiTreeExporterImpl
import com.family.farecompare.domain.automation.AppReturner
import com.family.farecompare.domain.inspector.UiInspectorRepository
import com.family.farecompare.domain.inspector.UiTreeDumper
import com.family.farecompare.domain.inspector.UiTreeExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InspectorModule {

    @Binds
    abstract fun bindUiTreeDumper(impl: UiTreeDumperImpl): UiTreeDumper

    @Binds
    abstract fun bindUiTreeExporter(impl: UiTreeExporterImpl): UiTreeExporter

    @Binds
    abstract fun bindUiInspectorRepository(impl: UiInspectorRepositoryImpl): UiInspectorRepository

    @Binds
    abstract fun bindAppReturner(impl: SelfAppReturnerImpl): AppReturner
}
