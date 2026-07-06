package com.example.growCare.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.example.growCare.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI State for HomeScreen
 */
data class HomeUiState(
    val user: User? = null,
    val aiTips: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingTips: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for HomeScreen
 * Manages local home data and AI tips
 * 
 * Follows MVVM architecture:
 * - Manages UI state with StateFlow
 * - Uses local-first defaults for GrowCare2
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadOfflineTips()
    }

    /**
     * GrowCare2 uses an offline-first assistant flow without weather dependency.
     */
    private fun loadOfflineTips() {
        _uiState.update {
            it.copy(
                aiTips = listOf(
                    "Field Check" to "Inspect leaves early in the morning for spots, discoloration, or wilting.",
                    "Watering" to "Prefer root-level irrigation and avoid wetting leaves late in the day.",
                    "Prevention" to "Remove infected leaves promptly and keep tools sanitized between plants."
                ),
                isLoadingTips = false
            )
        }
    }

    /**
     * Load local user profile for offline-first mode.
     */
    private fun loadUserData() {
        _uiState.update {
            it.copy(
                user = User(
                    uid = "local-user",
                    email = "local@growcare2",
                    displayName = "Farmer",
                    preferredCrops = emptyList()
                ),
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Refresh user data
     */
    fun refreshUserData() {
        loadUserData()
    }
}
