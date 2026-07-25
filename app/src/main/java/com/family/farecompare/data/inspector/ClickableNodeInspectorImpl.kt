package com.family.farecompare.data.inspector

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.inspector.ClickableNodeFormatter
import com.family.farecompare.domain.inspector.ClickableNodeInspector
import com.family.farecompare.domain.inspector.UiTreeDumper
import javax.inject.Inject

class ClickableNodeInspectorImpl @Inject constructor(
    private val uiTreeDumper: UiTreeDumper
) : ClickableNodeInspector {

    override fun inspect(rootNode: AccessibilityNodeInfo): String {
        val snapshot = uiTreeDumper.dump(rootNode)
        val clickableNodes = ClickableNodeFormatter.findClickableNodes(snapshot)
        return ClickableNodeFormatter.format(clickableNodes)
    }
}
