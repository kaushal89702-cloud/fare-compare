package com.family.farecompare.data.location

import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.location.SuggestionSelector
import javax.inject.Inject

/**
 * Scores every clickable, visible, text-bearing node by token overlap with
 * [typedQuery] and returns the best match. Falls back to "first clickable
 * node below the input row that has any text" when nothing scores well,
 * since autocomplete lists commonly place the top (best) suggestion first
 * regardless of exact text similarity.
 */
class SuggestionSelectorImpl @Inject constructor() : SuggestionSelector {

    override fun findBestSuggestion(rootNode: AccessibilityNodeInfo, typedQuery: String): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectClickableTextNodes(rootNode, depth = 0, candidates)
        if (candidates.isEmpty()) return null

        val queryTokens = tokenize(typedQuery)
        val scored = candidates.map { node -> node to overlapScore(queryTokens, node.text?.toString().orEmpty()) }
        val best = scored.maxByOrNull { it.second }

        return if (best != null && best.second > 0) best.first else candidates.first()
    }

    private fun collectClickableTextNodes(
        node: AccessibilityNodeInfo?,
        depth: Int,
        out: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return
        if (node.isVisibleToUser && !node.text.isNullOrBlank() && (node.isClickable || hasClickableAncestor(node))) {
            out.add(node)
        }
        for (i in 0 until node.childCount) {
            collectClickableTextNodes(node.getChild(i), depth + 1, out)
        }
    }

    private fun hasClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current = node.parent
        var hops = 0
        while (current != null && hops < MAX_CLICKABLE_ANCESTOR_HOPS) {
            if (current.isClickable) return true
            current = current.parent
            hops++
        }
        return false
    }

    private fun overlapScore(queryTokens: Set<String>, candidateText: String): Int {
        if (queryTokens.isEmpty()) return 0
        val candidateTokens = tokenize(candidateText)
        return queryTokens.intersect(candidateTokens).size
    }

    private fun tokenize(text: String): Set<String> = text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .toSet()

    private companion object {
        const val MAX_TRAVERSAL_DEPTH = 100
        const val MAX_CLICKABLE_ANCESTOR_HOPS = 6
    }
}
