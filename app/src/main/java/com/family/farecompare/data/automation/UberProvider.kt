package com.family.farecompare.data.automation

import com.family.farecompare.domain.foreground.ForegroundAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context

class UberProvider @Inject constructor(
    @ApplicationContext context: Context,
    foregroundAppRepository: ForegroundAppRepository
) : BaseRideAppProvider(context, foregroundAppRepository) {

    override val displayName: String = "Uber"
    override val packageName: String = "com.ubercab"

    // Uber commonly shows pickup as an always-editable field but shows
    // destination as a "Where to?" placeholder that must be tapped before
    // its real text input appears - handled generically by
    // LocationFieldDetector.findClickablePlaceholder using these hints.
    override val pickupFieldHints: List<String> = listOf("pickup location", "enter pickup location")
    override val destinationFieldHints: List<String> =
        listOf("where to?", "where to", "search destination", "enter destination", "confirm destination")
}
