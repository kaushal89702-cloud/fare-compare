package com.family.farecompare.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.family.farecompare.domain.model.PickupDetectionResult
import com.family.farecompare.domain.model.PickupVerificationStatus

/**
 * Developer-facing card showing the raw output of [LocationFieldDetector]
 * for the currently foregrounded ride provider. Not shown to end users in a
 * later release; useful now to verify detection quality across Uber, Ola,
 * and Rapido without needing logcat.
 */
@Composable
fun PickupDetectionCard(
    result: PickupDetectionResult?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp)) {
            Text(
                text = "Pickup Detection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (result == null) {
                DetectionRow(label = "Detected", value = "--")
                return@Column
            }

            DetectionRow(label = "Provider", value = result.providerDisplayName)
            DetectionRow(label = "Detected", value = if (result.detected) "Yes" else "No")
            DetectionRow(label = "Confidence Score", value = "${result.confidenceScore}%")
            DetectionRow(label = "Current Value", value = result.currentValue ?: "--")
            DetectionRow(
                label = "Bounds",
                value = result.bounds?.let { "(${it.left}, ${it.top}) - (${it.right}, ${it.bottom})" } ?: "--"
            )
            DetectionRow(label = "Verification", value = result.verification.toDisplayText())
        }
    }
}

@Composable
private fun DetectionRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

private fun PickupVerificationStatus.toDisplayText(): String = when (this) {
    PickupVerificationStatus.ALREADY_CORRECT -> "Pickup already correct"
    PickupVerificationStatus.NEEDS_UPDATE -> "Pickup needs update"
    PickupVerificationStatus.LOCATION_UNAVAILABLE -> "Location unavailable"
    PickupVerificationStatus.NOT_DETECTED -> "Not detected"
}
