package com.example.growCare.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.growCare.domain.model.ActivityStats
import com.example.growCare.domain.model.User
import com.example.growCare.domain.usecase.stats.GetActivityStatsUseCase
import com.example.growCare.domain.usecase.user.GetUserProfileUseCase
import com.example.growCare.domain.usecase.user.SaveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val activityStats: ActivityStats = ActivityStats(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ProfileAction {
    data object LoadProfile : ProfileAction
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getActivityStatsUseCase: GetActivityStatsUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadActivityStats()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.LoadProfile -> loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get user profile from local-first repository flow
                getUserProfileUseCase()
                    .collect { user ->
                        _uiState.update { state ->
                            state.copy(
                                user = user,
                                isLoading = false,
                                error = if (user == null) "No user profile found" else null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun loadActivityStats() {
        viewModelScope.launch {
            try {
                getActivityStatsUseCase().collect { stats ->
                    _uiState.update { it.copy(activityStats = stats) }
                }
            } catch (e: Exception) {
                // Silently fail for stats, don't show error to user
                _uiState.update { it.copy(activityStats = ActivityStats()) }
            }
        }
    }
}