package com.family.farecompare.data.automation

import com.family.farecompare.domain.foreground.ForegroundAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context

class RapidoProvider @Inject constructor(
    @ApplicationContext context: Context,
    foregroundAppRepository: ForegroundAppRepository
) : BaseRideAppProvider(context, foregroundAppRepository) {

    override val displayName: String = "Rapido"
    override val packageName: String = "com.rapido.passenger"

    override val pickupFieldHints: List<String> = listOf("your pickup location", "enter pickup point")
    override val destinationFieldHints: List<String> =
        listOf("enter your destination", "where do you want to go", "search drop location")
}
