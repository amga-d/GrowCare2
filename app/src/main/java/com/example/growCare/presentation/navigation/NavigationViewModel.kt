package com.example.growCare.presentation.navigation

import androidx.lifecycle.ViewModel
import com.example.growCare.domain.model.DiseaseAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Shared ViewModel for passing navigation data between screens
 * Avoids complex serialization and maintains type safety
 */
@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    // Disease Analysis State
    private val _currentDiseaseAnalysis = MutableStateFlow<DiseaseAnalysis?>(null)
    val currentDiseaseAnalysis: StateFlow<DiseaseAnalysis?> = _currentDiseaseAnalysis.asStateFlow()

    private val _currentDiseaseImageUrl = MutableStateFlow<String?>(null)
    val currentDiseaseImageUrl: StateFlow<String?> = _currentDiseaseImageUrl.asStateFlow()

    /**
     * Set disease analysis data for navigation
     */
    fun setDiseaseAnalysis(analysis: DiseaseAnalysis, imageUrl: String?) {
        _currentDiseaseAnalysis.value = analysis
        _currentDiseaseImageUrl.value = imageUrl
    }

    /**
     * Clear disease analysis data
     */
    fun clearDiseaseAnalysis() {
        _currentDiseaseAnalysis.value = null
        _currentDiseaseImageUrl.value = null
    }
}
