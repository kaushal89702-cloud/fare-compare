package com.family.farecompare.data.location

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.location.DetectedField
import com.family.farecompare.domain.location.LocationFieldDetector
import com.family.farecompare.domain.model.NodeBounds
import javax.inject.Inject

/**
 * Generic pickup-field detector that walks the accessibility node tree and
 * scores every editable candidate using signals that are common across
 * ride-hailing apps rather than any single provider's layout:
 *
 * - Is the node actually editable (an input, not a label)
 * - Does its text / hint / content description / resource id contain a
 *   pickup-related keyword ("pickup", "current location", "from", etc.)
 * - Do nearby sibling nodes carry a pickup-related label
 * - Is the node positioned in the upper portion of the screen (pickup
 *   fields conventionally precede destination fields)
 * - How shallow is the node in the hierarchy (mild preference)
 *
 * No resource IDs, class names, or layouts specific to Uber, Ola, or Rapido
 * are referenced, so this same implementation works for all of them.
 */
class LocationFieldDetectorImpl @Inject constructor() : LocationFieldDetector {

    override fun detectPickupField(rootNode: AccessibilityNodeInfo): DetectedField? {
        val candidates = mutableListOf<ScoredCandidate>()
        collectCandidates(rootNode, depth = 0, candidates = candidates)

        val best = candidates.maxByOrNull { it.score } ?: return null
        if (best.score < MINIMUM_SCORE_THRESHOLD) return null

        val confidence = ((best.score.toDouble() / MAX_POSSIBLE_SCORE) * 100)
            .toInt()
            .coerceIn(0, 100)

        return DetectedField(
            value = best.value,
            confidenceScore = confidence,
            bounds = best.bounds,
            resourceId = best.resourceId
        )
    }

    private fun collectCandidates(
        node: AccessibilityNodeInfo?,
        depth: Int,
        candidates: MutableList<ScoredCandidate>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return

        val isEditableLike = node.isEditable ||
            node.className?.toString()?.contains("EditText", ignoreCase = true) == true

        if (isEditableLike) {
            scoreCandidate(node, depth)?.let { candidates.add(it) }
        }

        for (i in 0 until node.childCount) {
            collectCandidates(node.getChild(i), depth + 1, candidates)
        }
    }

    private fun scoreCandidate(node: AccessibilityNodeInfo, depth: Int): ScoredCandidate? {
        if (!node.isVisibleToUser) return null

        val text = node.text?.toString().orEmpty()
        val hint = node.hintText?.toString().orEmpty()
        val contentDescription = node.contentDescription?.toString().orEmpty()
        val resourceId = node.viewIdResourceName.orEmpty()

        var score = 0
        if (node.isEditable) score += EDITABLE_WEIGHT
        if (node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
            score += CLASS_NAME_WEIGHT
        }
        score += keywordScore(text, TEXT_KEYWORD_WEIGHT)
        score += keywordScore(hint, HINT_KEYWORD_WEIGHT)
        score += keywordScore(contentDescription, DESCRIPTION_KEYWORD_WEIGHT)
        score += keywordScore(resourceId, RESOURCE_ID_KEYWORD_WEIGHT)
        score += nearbyLabelScore(node)

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.top in 0..screenTopRegionPx()) {
            score += POSITION_WEIGHT
        }

        score += (DEPTH_BONUS_MAX - depth).coerceIn(0, DEPTH_BONUS_MAX)

        if (score <= 0) return null

        val value = text.ifBlank { hint }.ifBlank { contentDescription }
        if (value.isBlank()) return null

        return ScoredCandidate(
            value = value,
            score = score,
            bounds = NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            resourceId = resourceId.ifBlank { null }
        )
    }

    private fun nearbyLabelScore(node: AccessibilityNodeInfo): Int {
        val parent = node.parent ?: return 0
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i) ?: continue
            if (sibling == node) continue
            val siblingText = (
                sibling.text?.toString().orEmpty() + " " + sibling.contentDescription?.toString().orEmpty()
                ).lowercase()
            if (PICKUP_KEYWORDS.any { siblingText.contains(it) }) {
                return NEARBY_LABEL_WEIGHT
            }
        }
        return 0
    }

    private fun keywordScore(source: String, weight: Int): Int {
        if (source.isBlank()) return 0
        val lower = source.lowercase()
        return if (PICKUP_KEYWORDS.any { lower.contains(it) }) weight else 0
    }

    /** Rough "upper portion of screen" cutoff, independent of any single device's resolution. */
    private fun screenTopRegionPx(): Int {
        val metrics = android.util.DisplayMetrics()
        return try {
            android.content.res.Resources.getSystem().displayMetrics.heightPixels / 2
        } catch (exception: Exception) {
            metrics.heightPixels.coerceAtLeast(1080) / 2
        }
    }

    private data class ScoredCandidate(
        val value: String,
        val score: Int,
        val bounds: NodeBounds,
        val resourceId: String?
    )

    private companion object {
        val PICKUP_KEYWORDS = listOf(
            "pickup",
            "pick up",
            "current location",
            "your location",
            "leaving from",
            "leaving  from",
            "source",
            "origin",
            "from location",
            "enter pickup",
            " from"
        )

        const val EDITABLE_WEIGHT = 40
        const val CLASS_NAME_WEIGHT = 10
        const val TEXT_KEYWORD_WEIGHT = 30
        const val HINT_KEYWORD_WEIGHT = 30
        const val DESCRIPTION_KEYWORD_WEIGHT = 25
        const val RESOURCE_ID_KEYWORD_WEIGHT = 20
        const val NEARBY_LABEL_WEIGHT = 15
        const val POSITION_WEIGHT = 15
        const val DEPTH_BONUS_MAX = 10

        const val MAX_POSSIBLE_SCORE = EDITABLE_WEIGHT + CLASS_NAME_WEIGHT + TEXT_KEYWORD_WEIGHT +
            HINT_KEYWORD_WEIGHT + DESCRIPTION_KEYWORD_WEIGHT + RESOURCE_ID_KEYWORD_WEIGHT +
            NEARBY_LABEL_WEIGHT + POSITION_WEIGHT + DEPTH_BONUS_MAX

        const val MINIMUM_SCORE_THRESHOLD = 35
        const val MAX_TRAVERSAL_DEPTH = 60
    }
}
