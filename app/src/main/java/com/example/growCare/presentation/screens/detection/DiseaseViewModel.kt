package com.example.growCare.presentation.screens.detection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.usecase.detection.AnalyzePlantDiseaseUseCase
import com.example.growCare.domain.usecase.detection.GetDiseaseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Disease Detection feature
 * Handles image capture, AI analysis, and result display
 */
@HiltViewModel
class DiseaseViewModel @Inject constructor(
    private val analyzePlantDiseaseUseCase: AnalyzePlantDiseaseUseCase,
    private val getDiseaseHistoryUseCase: GetDiseaseHistoryUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(DiseaseUiState())
    val uiState: StateFlow<DiseaseUiState> = _uiState.asStateFlow()

    // Events for one-time actions
    private val _events = MutableSharedFlow<DiseaseEvent>()
    val events: SharedFlow<DiseaseEvent> = _events.asSharedFlow()

    init {
        loadHistory()
    }

    /**
     * Handle user actions
     */
    fun onAction(action: DiseaseAction) {
        when (action) {
            is DiseaseAction.CaptureImage -> handleImageCapture(action.uri)
            is DiseaseAction.AnalyzeImage -> analyzeImage(action.uri)
            DiseaseAction.RetryAnalysis -> retryAnalysis()
            DiseaseAction.ClearResult -> clearResult()
            DiseaseAction.ShowCamera -> showCamera()
            DiseaseAction.HideCamera -> hideCamera()
        }
    }

    /**
     * Show camera for image capture
     */
    private fun showCamera() {
        _uiState.update { it.copy(
            showCamera = true,
            error = null
        )}
    }

    /**
     * Hide camera
     */
    private fun hideCamera() {
        _uiState.update { it.copy(showCamera = false) }
    }

    /**
     * Handle captured image
     */
    private fun handleImageCapture(uri: Uri) {
        _uiState.update { it.copy(
            capturedImageUri = uri,
            showCamera = false,
            error = null
        )}
        // Automatically start analysis
        analyzeImage(uri)
    }

    /**
     * Analyze plant disease from image
     */
    private fun analyzeImage(uri: Uri) {
        _uiState.update { it.copy(
            isAnalyzing = true,
            error = null,
            result = null
        )}

        viewModelScope.launch {
            try {
                analyzePlantDiseaseUseCase(uri)
                    .onSuccess { analysis ->
                        _uiState.update { it.copy(
                            isAnalyzing = false,
                            result = analysis,
                            error = null
                        )}
                        _events.emit(DiseaseEvent.AnalysisComplete(analysis))
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(
                            isAnalyzing = false,
                            error = error.message ?: "Analysis failed"
                        )}
                        _events.emit(DiseaseEvent.ShowError(error.message ?: "Analysis failed"))
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isAnalyzing = false,
                    error = e.message ?: "An error occurred"
                )}
                _events.emit(DiseaseEvent.ShowError(e.message ?: "An error occurred"))
            }
        }
    }

    /**
     * Retry last analysis
     */
    private fun retryAnalysis() {
        _uiState.value.capturedImageUri?.let { uri ->
            analyzeImage(uri)
        }
    }

    /**
     * Clear result and reset for new capture
     */
    private fun clearResult() {
        _uiState.update { DiseaseUiState() }
    }

    /**
     * Load analysis history
     */
    private fun loadHistory() {
        viewModelScope.launch {
            getDiseaseHistoryUseCase().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }
}

/**
 * UI State for Disease Detection screen
 */
data class DiseaseUiState(
    val capturedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val result: DiseaseAnalysis? = null,
    val error: String? = null,
    val showCamera: Boolean = false,
    val history: List<DiseaseAnalysis> = emptyList()
)

/**
 * User actions in Disease Detection screen
 */
sealed interface DiseaseAction {
    data class CaptureImage(val uri: Uri) : DiseaseAction
    data class AnalyzeImage(val uri: Uri) : DiseaseAction
    data object RetryAnalysis : DiseaseAction
    data object ClearResult : DiseaseAction
    data object ShowCamera : DiseaseAction
    data object HideCamera : DiseaseAction
}

/**
 * One-time events from ViewModel
 */
sealed interface DiseaseEvent {
    data class AnalysisComplete(val analysis: DiseaseAnalysis) : DiseaseEvent
    data class ShowError(val message: String) : DiseaseEvent
    data class ShowMessage(val message: String) : DiseaseEvent
}
