package com.family.farecompare.data.inspector

import com.family.farecompare.domain.inspector.UiInspectorRepository
import com.family.farecompare.domain.model.UiInspectorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiInspectorRepositoryImpl @Inject constructor() : UiInspectorRepository {

    private val _snapshot = MutableStateFlow<UiInspectorSnapshot?>(null)
    override val snapshot: StateFlow<UiInspectorSnapshot?> = _snapshot.asStateFlow()

    override fun updateSnapshot(snapshot: UiInspectorSnapshot) {
        _snapshot.value = snapshot
    }
}
