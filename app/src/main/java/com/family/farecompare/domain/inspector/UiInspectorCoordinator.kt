package com.family.farecompare.domain.inspector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.automation.AppReturner
import com.family.farecompare.domain.model.UiInspectorSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the "dump repeatedly while Uber's UI is still loading, stop once
 * it stabilizes" behavior described in the UI Inspector phase:
 *
 * - Every relevant accessibility event triggers an immediate re-dump
 *   (read-only; no clicks, typing, or gestures).
 * - Each dump is published live to [UiInspectorRepository] so the developer
 *   screen always shows the latest tree.
 * - A debounce timer resets on every new dump. Once no new dump arrives for
 *   [STABILIZATION_DEBOUNCE_MS], the UI is considered stable: the tree is
 *   exported to Documents/FareCompare and FareCompare is brought back to
 *   the foreground automatically.
 * - This "settle and finish" happens once per Uber session; leaving Uber's
 *   foreground resets it so the next time Uber opens, a fresh dump cycle runs.
 */
@Singleton
class UiInspectorCoordinator @Inject constructor(
    private val uiTreeDumper: UiTreeDumper,
    private val uiTreeExporter: UiTreeExporter,
    private val uiInspectorRepository: UiInspectorRepository,
    private val appReturner: AppReturner
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stabilizationJob: Job? = null
    private var lastFormattedTree: String? = null
    private var hasCompletedThisSession = false

    fun onRelevantUiEvent(rootNodeProvider: () -> AccessibilityNodeInfo?) {
        if (hasCompletedThisSession) return
        val rootNode = rootNodeProvider() ?: return

        coordinatorScope.launch {
            val snapshot = uiTreeDumper.dump(rootNode)
            val formattedTree = UiTreeFormatter.format(snapshot)
            val nodeCount = UiTreeFormatter.countNodes(snapshot)
            val changedSinceLastDump = formattedTree != lastFormattedTree
            lastFormattedTree = formattedTree

            uiInspectorRepository.updateSnapshot(
                UiInspectorSnapshot(
                    treeText = formattedTree,
                    nodeCount = nodeCount,
                    isStabilized = false,
                    exportedFilePath = null,
                    lastUpdatedAtMillis = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Dumped Uber UI tree: $nodeCount nodes, changed=$changedSinceLastDump")

            stabilizationJob?.cancel()
            stabilizationJob = launch {
                delay(STABILIZATION_DEBOUNCE_MS)
                finalizeIfStillCurrent(formattedTree, nodeCount)
            }
        }
    }

    fun onForegroundAppChanged(packageName: String) {
        if (packageName != UBER_PACKAGE_NAME) {
            stabilizationJob?.cancel()
            hasCompletedThisSession = false
            lastFormattedTree = null
        }
    }

    private suspend fun finalizeIfStillCurrent(formattedTree: String, nodeCount: Int) {
        if (hasCompletedThisSession || formattedTree != lastFormattedTree) return

        Log.d(TAG, "Uber UI stabilized ($nodeCount nodes) - exporting")
        val exportedPath = uiTreeExporter.exportToDocuments(EXPORT_FILE_NAME, formattedTree)

        uiInspectorRepository.updateSnapshot(
            UiInspectorSnapshot(
                treeText = formattedTree,
                nodeCount = nodeCount,
                isStabilized = true,
                exportedFilePath = exportedPath,
                lastUpdatedAtMillis = System.currentTimeMillis()
            )
        )
        hasCompletedThisSession = true

        Log.d(TAG, "Returning to FareCompare")
        appReturner.bringFareCompareToForeground()
    }

    private companion object {
        const val TAG = "UiInspectorCoordinator"
        const val STABILIZATION_DEBOUNCE_MS = 1_500L
        const val EXPORT_FILE_NAME = "uber_ui_tree.txt"
        const val UBER_PACKAGE_NAME = "com.ubercab"
    }
}
