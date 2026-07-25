package com.family.farecompare.domain.automation

import com.family.farecompare.domain.accessibility.AccessibilityStatusChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val FOREGROUND_WAIT_TIMEOUT_MS = 10_000L

/**
 * Orchestrates launching a single [RideAppProvider]:
 * accessibility check -> installed check -> launch -> wait for foreground.
 *
 * This use case is provider-agnostic: adding Ola or Rapido support only
 * requires a new [RideAppProvider] implementation, never a change here.
 */
class LaunchRideAppUseCase @Inject constructor(
    private val accessibilityStatusChecker: AccessibilityStatusChecker
) {
    operator fun invoke(provider: RideAppProvider): Flow<AutomationStep> = flow {
        try {
            emit(AutomationStep.CheckingAccessibility)
            if (!accessibilityStatusChecker.isServiceEnabled()) {
                emit(AutomationStep.AccessibilityDisabled)
                return@flow
            }

            emit(AutomationStep.CheckingInstalled)
            if (!provider.isInstalled()) {
                emit(AutomationStep.NotInstalled)
                return@flow
            }

            emit(AutomationStep.Launching)
            provider.launch()

            emit(AutomationStep.Waiting)
            val reachedForeground = provider.waitUntilForeground(FOREGROUND_WAIT_TIMEOUT_MS)

            emit(if (reachedForeground) AutomationStep.Success else AutomationStep.Timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            emit(AutomationStep.UnexpectedError(exception.message ?: exception.javaClass.simpleName))
        }
    }
}
