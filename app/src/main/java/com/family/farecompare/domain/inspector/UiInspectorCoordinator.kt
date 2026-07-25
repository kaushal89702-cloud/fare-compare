package com.family.farecompare.domain.inspector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.automation.AppReturner
import com.family.farecompare.domain.foreground.KnownForegroundApps
import com.family.farecompare.domain.model.RideProvider
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
 * Drives the "dump repeatedly while the ride app's UI is still loading,
 * stop once it stabilizes" behavior of the developer UI Inspector, for
 * *any* of the three supported ride providers (Uber, Ola, Rapido) - not
 * Uber specifically. Whichever provider's package is currently in the
 * foreground is what gets dumped:
 *
 * - Every relevant accessibility event triggers an immediate re-dump
 *   (read-only; no clicks, typing, or gestures).
 * - Each dump is published live to [UiInspectorRepository] so the developer
 *   screen always shows the latest tree.
 * - A debounce timer resets on every new dump. Once no new dump arrives for
 *   [STABILIZATION_DEBOUNCE_MS], the UI is considered stable: the tree is
 *   exported to Documents/FareCompare and FareCompare is brought back to
 *   the foreground automatically.
 * - This "settle and finish" happens once per provider session; leaving
 *   that provider's foreground resets it so the next time any ride
 *   provider opens, a fresh dump cycle runs.
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
    private var activePackageName: String? = null

    fun onRelevantUiEvent(packageName: String, rootNodeProvider: () -> AccessibilityNodeInfo?) {
        if (!isKnownRideProviderPackage(packageName)) return
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
            val providerName = KnownForegroundApps.displayNameFor(packageName) ?: packageName
            Log.d(TAG, "Dumped $providerName UI tree: $nodeCount nodes, changed=$changedSinceLastDump")

            stabilizationJob?.cancel()
            stabilizationJob = launch {
                delay(STABILIZATION_DEBOUNCE_MS)
                finalizeIfStillCurrent(packageName, formattedTree, nodeCount)
            }
        }
    }

    fun onForegroundAppChanged(packageName: String) {
        if (packageName != activePackageName) {
            stabilizationJob?.cancel()
            hasCompletedThisSession = false
            lastFormattedTree = null
            activePackageName = if (isKnownRideProviderPackage(packageName)) packageName else null
        }
    }

    private fun isKnownRideProviderPackage(packageName: String): Boolean =
        RideProvider.values().any { it.packageName == packageName }

    private suspend fun finalizeIfStillCurrent(packageName: String, formattedTree: String, nodeCount: Int) {
        if (hasCompletedThisSession || formattedTree != lastFormattedTree) return

        val providerName = KnownForegroundApps.displayNameFor(packageName) ?: packageName
        Log.d(TAG, "$providerName UI stabilized ($nodeCount nodes) - exporting")
        val fileName = "${providerName.lowercase()}_ui_tree.txt"
        val exportedPath = uiTreeExporter.exportToDocuments(fileName, formattedTree)

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
    }
}
