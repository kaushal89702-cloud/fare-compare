package com.family.farecompare.data.fare

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.family.farecompare.domain.fare.DetectedFare
import com.family.farecompare.domain.fare.FareExtractor
import com.family.farecompare.domain.model.NodeBounds
import com.family.farecompare.domain.model.RideType
import javax.inject.Inject
import kotlin.math.abs

/**
 * Generic implementation of [FareExtractor]. See the interface doc for the
 * overall strategy; this class focuses on:
 *
 * 1. Finding every text node that parses as a currency amount
 *    ([FareTextParser]).
 * 2. Classifying the nearest ride-tier label among that node's siblings and
 *    ancestor-siblings ([RideType]).
 * 3. Picking up an ETA ("12 min", "3 mins away") from the same neighborhood.
 * 4. De-duplicating near-identical readings that stem from the same visual
 *    row being reported by more than one overlapping node.
 */
class FareExtractorImpl @Inject constructor() : FareExtractor {

    override fun extractFares(rootNode: AccessibilityNodeInfo): List<DetectedFare> {
        val textNodes = mutableListOf<AccessibilityNodeInfo>()
        collectTextNodes(rootNode, depth = 0, textNodes)

        val fares = textNodes.mapNotNull { node -> tryExtractFareFromNode(node, textNodes) }

        return deduplicate(fares)
    }

    private fun collectTextNodes(
        node: AccessibilityNodeInfo?,
        depth: Int,
        out: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH) return
        if (node.isVisibleToUser && !node.text.isNullOrBlank()) {
            out.add(node)
        }
        for (i in 0 until node.childCount) {
            collectTextNodes(node.getChild(i), depth + 1, out)
        }
    }

    private fun tryExtractFareFromNode(
        node: AccessibilityNodeInfo,
        allTextNodes: List<AccessibilityNodeInfo>
    ): DetectedFare? {
        val text = node.text?.toString() ?: return null
        val parsedAmount = FareTextParser.parseAmount(text) ?: return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val nearbyTexts = findNearbyTexts(bounds, allTextNodes, excluding = node)
        val rideType = classifyRideType(nearbyTexts, node.parent)
        val etaMinutes = nearbyTexts.firstNotNullOfOrNull { FareTextParser.parseEtaMinutes(it) }

        val confidence = if (rideType != RideType.UNKNOWN) HIGH_CONFIDENCE else LOW_CONFIDENCE

        return DetectedFare(
            rideType = rideType,
            amount = parsedAmount,
            rawText = text.trim(),
            etaMinutes = etaMinutes,
            bounds = NodeBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            confidenceScore = confidence
        )
    }

    /** Text from nodes whose bounds are vertically close to [targetBounds] (same row/card). */
    private fun findNearbyTexts(
        targetBounds: Rect,
        allTextNodes: List<AccessibilityNodeInfo>,
        excluding: AccessibilityNodeInfo
    ): List<String> {
        val results = mutableListOf<String>()
        for (candidate in allTextNodes) {
            if (candidate == excluding) continue
            val candidateBounds = Rect()
            candidate.getBoundsInScreen(candidateBounds)
            val verticalDistance = abs(candidateBounds.centerY() - targetBounds.centerY())
            if (verticalDistance <= NEARBY_ROW_TOLERANCE_PX) {
                candidate.text?.toString()?.let { results.add(it) }
            }
        }
        return results
    }

    private fun classifyRideType(nearbyTexts: List<String>, parent: AccessibilityNodeInfo?): RideType {
        val combined = (nearbyTexts + listOfNotNull(parent?.text?.toString(), parent?.contentDescription?.toString()))
            .joinToString(" ")
            .lowercase()

        return RIDE_TYPE_KEYWORDS.entries.firstOrNull { (_, keywords) ->
            keywords.any { combined.contains(it) }
        }?.key ?: RideType.UNKNOWN
    }

    /**
     * Multiple overlapping accessibility nodes (a container + its text
     * child, for example) can report the same visual fare twice. Collapse
     * readings that share a ride type and an amount within a very small
     * tolerance, keeping only the highest-confidence one.
     */
    private fun deduplicate(fares: List<DetectedFare>): List<DetectedFare> {
        val kept = mutableListOf<DetectedFare>()
        for (fare in fares.sortedByDescending { it.confidenceScore }) {
            val isDuplicate = kept.any { existing ->
                existing.rideType == fare.rideType && abs(existing.amount - fare.amount) < 0.01
            }
            if (!isDuplicate) kept.add(fare)
        }
        return kept
    }

    private companion object {
        const val MAX_TRAVERSAL_DEPTH = 100
        const val NEARBY_ROW_TOLERANCE_PX = 220
        const val HIGH_CONFIDENCE = 90
        const val LOW_CONFIDENCE = 50

        val RIDE_TYPE_KEYWORDS: Map<RideType, List<String>> = mapOf(
            RideType.BIKE to listOf("bike", "moto"),
            RideType.AUTO to listOf("auto", "rickshaw"),
            RideType.MINI to listOf("mini", "go", "lite"),
            RideType.PREMIUM to listOf("premium", "prime", "luxury", "sedan", "xl"),
            RideType.CAB to listOf("cab", "car", "taxi")
        )
    }
}
