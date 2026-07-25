package com.family.farecompare.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.family.farecompare.domain.settings.ThemeMode
import com.family.farecompare.presentation.comparison.ComparisonScreen
import com.family.farecompare.presentation.home.HomeScreen
import com.family.farecompare.presentation.inspector.InspectorScreen
import com.family.farecompare.presentation.permissions.PermissionsScreen
import com.family.farecompare.presentation.settings.SettingsScreen
import com.family.farecompare.presentation.theme.FareCompareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FareCompareTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FareCompareNavHost()
                }
            }
        }
    }
}

private object FareCompareDestinations {
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val INSPECTOR = "inspector"
    const val COMPARISON = "comparison"
}

@Composable
private fun FareCompareNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = FareCompareDestinations.PERMISSIONS) {
        composable(FareCompareDestinations.PERMISSIONS) {
            PermissionsScreen(
                onContinueClick = {
                    navController.navigate(FareCompareDestinations.HOME) {
                        popUpTo(FareCompareDestinations.PERMISSIONS) { inclusive = true }
                    }
                },
                onOpenAccessibilitySettings = {
                    navController.context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                }
            )
        }
        composable(FareCompareDestinations.HOME) {
            HomeScreen(
                onSettingsClick = { navController.navigate(FareCompareDestinations.SETTINGS) },
                onInspectorClick = { navController.navigate(FareCompareDestinations.INSPECTOR) },
                onCompareStarted = { navController.navigate(FareCompareDestinations.COMPARISON) }
            )
        }
        composable(FareCompareDestinations.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(FareCompareDestinations.INSPECTOR) {
            InspectorScreen(onBackClick = { navController.popBackStack() })
        }
        composable(FareCompareDestinations.COMPARISON) {
            ComparisonScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
