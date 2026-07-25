package com.family.farecompare.data.inspector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.inspector.FailureDiagnosticsRecorder
import com.family.farecompare.domain.inspector.FailureDiagnosticsSummary
import com.family.farecompare.domain.inspector.UiTreeDumper
import com.family.farecompare.domain.inspector.UiTreeExporter
import com.family.farecompare.domain.inspector.UiTreeFormatter
import com.family.farecompare.domain.model.UiNodeSnapshot
import javax.inject.Inject

class FailureDiagnosticsRecorderImpl @Inject constructor(
    private val uiTreeDumper: UiTreeDumper,
    private val uiTreeExporter: UiTreeExporter
) : FailureDiagnosticsRecorder {

    override suspend fun captureFailure(
        providerDisplayName: String,
        reason: String,
        rootNode: AccessibilityNodeInfo?
    ): FailureDiagnosticsSummary {
        if (rootNode == null) {
            Log.w(TAG, "[$providerDisplayName] Failure ($reason): no root node available to capture")
            return FailureDiagnosticsSummary(exportedFilePath = null, editableFieldDescriptions = emptyList(), totalNodeCount = 0)
        }

        val snapshot = uiTreeDumper.dump(rootNode)
        val formattedTree = UiTreeFormatter.format(snapshot)
        val nodeCount = UiTreeFormatter.countNodes(snapshot)
        val fileName = "${providerDisplayName.lowercase()}_failure_${System.currentTimeMillis()}.txt"
        val exportedPath = uiTreeExporter.exportToDocuments(fileName, formattedTree)

        val editableDescriptions = mutableListOf<String>()
        collectEditableDescriptions(snapshot, editableDescriptions)

        Log.w(
            TAG,
            "[$providerDisplayName] Failure captured ($reason): $nodeCount nodes, " +
                "${editableDescriptions.size} editable fields found, exported to $exportedPath"
        )
        editableDescriptions.forEach { Log.w(TAG, "[$providerDisplayName] Editable field: $it") }

        return FailureDiagnosticsSummary(
            exportedFilePath = exportedPath,
            editableFieldDescriptions = editableDescriptions,
            totalNodeCount = nodeCount
        )
    }

    private fun collectEditableDescriptions(node: UiNodeSnapshot, out: MutableList<String>) {
        if (node.editable) {
            out.add(
                "resourceId=${node.resourceId ?: "-"} text='${node.text.orEmpty()}' " +
                    "hint='${node.hintText.orEmpty()}' contentDesc='${node.contentDescription.orEmpty()}' " +
                    "bounds=(${node.bounds.left},${node.bounds.top})-(${node.bounds.right},${node.bounds.bottom})"
            )
        }
        node.children.forEach { collectEditableDescriptions(it, out) }
    }

    private companion object {
        const val TAG = "FailureDiagnostics"
    }
}
