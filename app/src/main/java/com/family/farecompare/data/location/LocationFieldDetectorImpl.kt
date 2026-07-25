package com.family.farecompare.data.location

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.location.DetectedField
import com.family.farecompare.domain.location.FieldRole
import com.family.farecompare.domain.location.LocationFieldDetector
import com.family.farecompare.domain.model.NodeBounds
import javax.inject.Inject

/**
 * Generic location-field detector that walks the accessibility node tree and
 * scores every editable candidate using signals that are common across
 * ride-hailing apps rather than any single provider's layout:
 *
 * - Is the node actually editable (an input, not a label)
 * - Does its text / hint / content description / resource id contain a
 *   role-specific keyword ("pickup", "current location", "where to", etc.),
 *   plus any provider-supplied [DetectedField]-independent extra keywords
 * - Do nearby sibling nodes carry a matching label
 * - Is the node positioned in the upper portion of the screen (pickup
 *   fields conventionally precede destination fields; this bonus is only
 *   applied for [FieldRole.PICKUP])
 * - How shallow is the node in the hierarchy (mild preference)
 *
 * No resource IDs, class names, or layouts specific to Uber, Ola, or Rapido
 * are hardcoded here - provider wording differences are supplied by the
 * caller via `additionalKeywords`, keeping this implementation shared.
 */
class LocationFieldDetectorImpl @Inject constructor() : LocationFieldDetector {

    override fun detectField(
        rootNode: AccessibilityNodeInfo,
        role: FieldRole,
        additionalKeywords: List<String>
    ): DetectedField? {
        val keywords = keywordsFor(role) + additionalKeywords.map { it.lowercase() }
        val candidates = mutableListOf<ScoredCandidate>()
        collectEditableCandidates(rootNode, depth = 0, role = role, keywords = keywords, candidates = candidates)

        val best = candidates.maxByOrNull { it.score } ?: return null
        if (best.score < MINIMUM_SCORE_THRESHOLD) return null

        return best.toDetectedField()
    }

    override fun findClickablePlaceholder(
        rootNode: AccessibilityNodeInfo,
        role: FieldRole,
        additionalKeywords: List<String>
    ): DetectedField? {
        val keywords = keywordsFor(role) + additionalKeywords.map { it.lowercase() }
        val candidates = mutableListOf<ScoredCandidate>()
        collectClickablePlaceholderCandidates(rootNode, depth = 0, keywords = keywords, candidates = candidates)

        val best = candidates.maxByOrNull { it.score } ?: return null
        if (best.score < MINIMUM_PLACEHOLDER_SCORE_THRESHOLD) return null

        return best.toDetectedField()
    }

    override fun findAnyEditableField(rootNode: AccessibilityNodeInfo, excludeBounds: NodeBounds?): DetectedField? {
        val candidates = mutableListOf<ScoredCandidate>()
        collectAnyEditable(rootNode, depth = 0, excludeBounds = excludeBounds, candidates = candidates)

        // Prefer a still-blank field (no text yet) since a field the user
        // already filled for another role is a poor fallback target; fall
        // back to any editable field if every candidate already has text.
        val best = candidates.filter { it.isBlank }.maxByOrNull { it.score }
            ?: candidates.maxByOrNull { it.score }
            ?: return null

        return best.toDetectedField()
    }

    private fun collectEditableCandidates(
        node: AccessibilityNodeInfo?,
        depth: Int,
        role: FieldRole,
        keywords: List<String>,
        candidates: MutableList<ScoredCandidate>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return

        if (isEditableLike(node)) {
            scoreEditableCandidate(node, depth, role, keywords)?.let { candidates.add(it) }
        }

        for (i in 0 until node.childCount) {
            collectEditableCandidates(node.getChild(i), depth + 1, role, keywords, candidates)
        }
    }

    private fun collectClickablePlaceholderCandidates(
        node: AccessibilityNodeInfo?,
        depth: Int,
        keywords: List<String>,
        candidates: MutableList<ScoredCandidate>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return

        if (!isEditableLike(node) && node.isVisibleToUser && (node.isClickable || node.isFocusable)) {
            scorePlaceholderCandidate(node, depth, keywords)?.let { candidates.add(it) }
        }

        for (i in 0 until node.childCount) {
            collectClickablePlaceholderCandidates(node.getChild(i), depth + 1, keywords, candidates)
        }
    }

    private fun collectAnyEditable(
        node: AccessibilityNodeInfo?,
        depth: Int,
        excludeBounds: NodeBounds?,
        candidates: MutableList<ScoredCandidate>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return

        if (isEditableLike(node) && node.isVisibleToUser) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val nodeBounds = NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            if (excludeBounds == null || nodeBounds != excludeBounds) {
                val text = node.text?.toString().orEmpty()
                val hint = node.hintText?.toString().orEmpty()
                val contentDescription = node.contentDescription?.toString().orEmpty()
                val value = text.ifBlank { hint }.ifBlank { contentDescription }.ifBlank { "(empty field)" }
                candidates.add(
                    ScoredCandidate(
                        value = value,
                        score = (DEPTH_BONUS_MAX - depth).coerceIn(0, DEPTH_BONUS_MAX) + EDITABLE_WEIGHT,
                        bounds = nodeBounds,
                        resourceId = node.viewIdResourceName,
                        isBlank = text.isBlank()
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            collectAnyEditable(node.getChild(i), depth + 1, excludeBounds, candidates)
        }
    }

    private fun isEditableLike(node: AccessibilityNodeInfo): Boolean =
        node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true

    private fun scoreEditableCandidate(
        node: AccessibilityNodeInfo,
        depth: Int,
        role: FieldRole,
        keywords: List<String>
    ): ScoredCandidate? {
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
        score += keywordScore(text, keywords, TEXT_KEYWORD_WEIGHT)
        score += keywordScore(hint, keywords, HINT_KEYWORD_WEIGHT)
        score += keywordScore(contentDescription, keywords, DESCRIPTION_KEYWORD_WEIGHT)
        score += keywordScore(resourceId, keywords, RESOURCE_ID_KEYWORD_WEIGHT)
        score += nearbyLabelScore(node, keywords)

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (role == FieldRole.PICKUP && bounds.top in 0..screenTopRegionPx()) {
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
            resourceId = resourceId.ifBlank { null },
            isBlank = text.isBlank()
        )
    }

    private fun scorePlaceholderCandidate(
        node: AccessibilityNodeInfo,
        depth: Int,
        keywords: List<String>
    ): ScoredCandidate? {
        val text = node.text?.toString().orEmpty()
        val contentDescription = node.contentDescription?.toString().orEmpty()
        val resourceId = node.viewIdResourceName.orEmpty()

        var score = 0
        score += keywordScore(text, keywords, TEXT_KEYWORD_WEIGHT)
        score += keywordScore(contentDescription, keywords, DESCRIPTION_KEYWORD_WEIGHT)
        score += keywordScore(resourceId, keywords, RESOURCE_ID_KEYWORD_WEIGHT)
        if (node.isClickable) score += EDITABLE_WEIGHT / 2
        score += (DEPTH_BONUS_MAX - depth).coerceIn(0, DEPTH_BONUS_MAX)

        if (score <= 0) return null
        val value = text.ifBlank { contentDescription }
        if (value.isBlank()) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return ScoredCandidate(
            value = value,
            score = score,
            bounds = NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            resourceId = resourceId.ifBlank { null },
            isBlank = true
        )
    }

    private fun nearbyLabelScore(node: AccessibilityNodeInfo, keywords: List<String>): Int {
        val parent = node.parent ?: return 0
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i) ?: continue
            if (sibling == node) continue
            val siblingText = (
                sibling.text?.toString().orEmpty() + " " + sibling.contentDescription?.toString().orEmpty()
                ).lowercase()
            if (keywords.any { siblingText.contains(it) }) {
                return NEARBY_LABEL_WEIGHT
            }
        }
        return 0
    }

    private fun keywordScore(source: String, keywords: List<String>, weight: Int): Int {
        if (source.isBlank()) return 0
        val lower = source.lowercase()
        return if (keywords.any { lower.contains(it) }) weight else 0
    }

    private fun keywordsFor(role: FieldRole): List<String> = when (role) {
        FieldRole.PICKUP -> PICKUP_KEYWORDS
        FieldRole.DESTINATION -> DESTINATION_KEYWORDS
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
        val resourceId: String?,
        val isBlank: Boolean
    ) {
        fun toDetectedField(): DetectedField {
            val confidence = ((score.toDouble() / MAX_POSSIBLE_SCORE) * 100).toInt().coerceIn(0, 100)
            return DetectedField(value = value, confidenceScore = confidence, bounds = bounds, resourceId = resourceId)
        }
    }

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

        val DESTINATION_KEYWORDS = listOf(
            "destination",
            "drop",
            "where to",
            "where are you going",
            "going to",
            "enter destination",
            "to location",
            " to"
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
        const val MINIMUM_PLACEHOLDER_SCORE_THRESHOLD = 20
        const val MAX_TRAVERSAL_DEPTH = 60
    }
}
