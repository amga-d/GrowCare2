package com.example.growCare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.growCare.presentation.navigation.NavGraph
import com.example.growCare.presentation.navigation.Screen
import com.example.growCare.presentation.screens.home.HomeScreen
import com.example.growCare.presentation.theme.ThemeViewModel
import com.example.growCare.presentation.theme.shouldUseDarkTheme
import com.example.growCare.ui.theme.MobileAppDevTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * GrowCare2 - Offline-First Smart Agriculture Assistant
 * Mobile Application Development Final Project
 * 
 * Group Members:
 * 1. Amgad Aref Abdulrazzaq Derhem Al-Ameri - 23523242
 * 2. Ahnaf Rafi Raditya - 23523070
 * 3. Dhani Fatih Ilyasa - 23523172
 * 4. Ravfael Novfito Handoyo - 23523257
 * 
 * Build Requirements:
 * - JVM Target: 17 (Java 17)
 * - Android Studio: Hedgehog or later
 * - Gradle: 8.7+
 * - Minimum SDK: 24 (Android 7.0)
 * - Target SDK: 36 (Android 14)
 * 
 * Note: If you encounter build issues, ensure you're using JDK 17 or later.
 * Check your JAVA_HOME environment variable and Android Studio's JDK settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val isDarkTheme = shouldUseDarkTheme(themeMode)
            
            MobileAppDevTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                NavGraph(
                    navController = navController,
                    startDestination = Screen.HOME,
                    themeMode = themeMode,
                    onThemeModeChange = { themeViewModel.setThemeMode(it) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MobileAppDevTheme {
        HomeScreen()
    }
}