package com.example.growCare.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.growCare.data.local.preferences.ThemeMode
import com.example.growCare.presentation.screens.chat.ChatScreen
import com.example.growCare.presentation.screens.detection.DiseaseScanScreen
import com.example.growCare.presentation.screens.detection.DiseaseResultScreen
import com.example.growCare.presentation.screens.home.HomeScreen
import com.example.growCare.presentation.screens.profile.ProfileScreen
import com.example.growCare.presentation.screens.profile.EditProfileScreen
import kotlinx.coroutines.launch

/**
 * Navigation graph for the GrowCare application
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    navigationViewModel: NavigationViewModel = hiltViewModel(),
    startDestination: String = Screen.HOME,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home screen
        composable(Screen.HOME) {
            HomeScreen(
                onNavigateToDiseaseScan = {
                    navController.navigate(Screen.DISEASE_SCAN)
                },
                onNavigateToChat = {
                    navController.navigate("chat")
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.PROFILE)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.HISTORY)
                }
            )
        }

        // Disease detection
        composable(Screen.DISEASE_SCAN) {
            DiseaseScanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResult = { analysis, imageUrl ->
                    navigationViewModel.setDiseaseAnalysis(analysis, imageUrl)
                    navController.navigate(Screen.DISEASE_RESULT)
                }
            )
        }

        // Disease detection result
        composable(Screen.DISEASE_RESULT) {
            val analysis by navigationViewModel.currentDiseaseAnalysis.collectAsStateWithLifecycle()
            val imageUrl by navigationViewModel.currentDiseaseImageUrl.collectAsStateWithLifecycle()
            
            analysis?.let { diseaseAnalysis ->
                DiseaseResultScreen(
                    analysis = diseaseAnalysis,
                    imageUrl = imageUrl,
                    onNavigateBack = {
                        navigationViewModel.clearDiseaseAnalysis()
                        navController.popBackStack()
                    },
                    onScanAnother = {
                        navigationViewModel.clearDiseaseAnalysis()
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navigationViewModel.clearDiseaseAnalysis()
                        navController.navigate(Screen.HOME) {
                            popUpTo(Screen.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }

        // AI Chat assistant
        composable(Screen.CHAT) {
            val chatViewModel: com.example.growCare.presentation.screens.chat.ChatViewModel = hiltViewModel()
            
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.PROFILE)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.CHAT_HISTORY)
                }
            )
        }

        // User profile
        composable(Screen.PROFILE) {
            ProfileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EDIT_PROFILE)
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
        
        // Edit Profile
        composable(Screen.EDIT_PROFILE) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Activity History
        composable(Screen.HISTORY) {
            val historyViewModel: com.example.growCare.presentation.screens.history.HistoryViewModel = hiltViewModel()
            val scope = rememberCoroutineScope()
            
            com.example.growCare.presentation.screens.history.HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDiseaseResult = { diseaseId ->
                    scope.launch {
                        val analysis = historyViewModel.loadDiseaseById(diseaseId)
                        if (analysis != null) {
                            navigationViewModel.setDiseaseAnalysis(analysis, analysis.imageUrl)
                            navController.navigate(Screen.DISEASE_RESULT)
                        }
                    }
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate("chat?conversationId=$conversationId")
                }
            )
        }
        
        // Chat History
        composable(Screen.CHAT_HISTORY) {
            val chatViewModel: com.example.growCare.presentation.screens.chat.ChatViewModel = hiltViewModel()
            val scope = rememberCoroutineScope()
            
            com.example.growCare.presentation.screens.chat.ChatHistoryScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConversationClick = { conversationId ->
                    // Navigate to chat with conversation ID
                    navController.navigate("chat?conversationId=$conversationId")
                }
            )
        }
    }
}
