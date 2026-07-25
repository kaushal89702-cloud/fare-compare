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
}
