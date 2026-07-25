package com.family.farecompare.domain.automation

import com.family.farecompare.domain.foreground.ForegroundAppRepository
import com.family.farecompare.domain.model.RideProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val FOREGROUND_WAIT_TIMEOUT_MS = 10_000L

/**
 * Launches a ride provider app (if installed) and, using the accessibility
 * service's foreground-app events (no polling), waits up to
 * [FOREGROUND_WAIT_TIMEOUT_MS] for it to become the foreground application.
 */
class LaunchRideAppUseCase @Inject constructor(
    private val appLauncher: AppLauncher,
    private val foregroundAppRepository: ForegroundAppRepository
) {
    operator fun invoke(provider: RideProvider): Flow<AutomationStep> = flow {
        if (!appLauncher.isAppInstalled(provider.packageName)) {
            emit(AutomationStep.NotInstalled)
            return@flow
        }

        emit(AutomationStep.Launching)
        appLauncher.launchApp(provider.packageName)

        emit(AutomationStep.Waiting)
        val reachedForeground = withTimeoutOrNull(FOREGROUND_WAIT_TIMEOUT_MS) {
            foregroundAppRepository.currentForegroundApp
                .filterNotNull()
                .first { it.packageName == provider.packageName }
        }

        emit(if (reachedForeground != null) AutomationStep.Success else AutomationStep.Timeout)
    }
}
