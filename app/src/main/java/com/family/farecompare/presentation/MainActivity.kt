package com.family.farecompare.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.family.farecompare.presentation.home.HomeScreen
import com.family.farecompare.presentation.inspector.InspectorScreen
import com.family.farecompare.presentation.settings.SettingsScreen
import com.family.farecompare.presentation.theme.FareCompareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FareCompareTheme {
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
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val INSPECTOR = "inspector"
}

@Composable
private fun FareCompareNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = FareCompareDestinations.HOME) {
        composable(FareCompareDestinations.HOME) {
            HomeScreen(
                onSettingsClick = { navController.navigate(FareCompareDestinations.SETTINGS) },
                onInspectorClick = { navController.navigate(FareCompareDestinations.INSPECTOR) }
            )
        }
        composable(FareCompareDestinations.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(FareCompareDestinations.INSPECTOR) {
            InspectorScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
