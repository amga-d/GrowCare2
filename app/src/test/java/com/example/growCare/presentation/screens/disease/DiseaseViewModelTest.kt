package com.example.growCare.presentation.screens.disease

import android.net.Uri
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.model.DiseaseSeverity
import com.example.growCare.domain.usecase.detection.AnalyzePlantDiseaseUseCase
import com.example.growCare.domain.usecase.detection.GetDiseaseHistoryUseCase
import com.example.growCare.presentation.screens.detection.DiseaseAction
import com.example.growCare.presentation.screens.detection.DiseaseEvent
import com.example.growCare.presentation.screens.detection.DiseaseViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiseaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: DiseaseViewModel
    private lateinit var analyzePlantDiseaseUseCase: AnalyzePlantDiseaseUseCase
    private lateinit var getDiseaseHistoryUseCase: GetDiseaseHistoryUseCase
    
    private val mockUri = mockk<Uri>(relaxed = true)
    private val mockAnalysis = DiseaseAnalysis(
        id = "test_id",
        userId = "test_user",
        cropName = "Tomato",
        imageUrl = "https://example.com/image.jpg",
        diseaseName = "Leaf Spot",
        confidence = 85,
        symptoms = listOf("Brown spots on leaves", "Yellowing"),
        severity = DiseaseSeverity.MODERATE,
        treatment = listOf("Remove affected leaves", "Apply fungicide"),
        prevention = listOf("Proper spacing", "Water at base"),
        additionalNotes = "Early detection"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        analyzePlantDiseaseUseCase = mockk()
        getDiseaseHistoryUseCase = mockk()
        
        // Mock history use case that's called in init
        every { getDiseaseHistoryUseCase() } returns flowOf(emptyList())
        
        viewModel = DiseaseViewModel(analyzePlantDiseaseUseCase, getDiseaseHistoryUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        
        assertNull(state.capturedImageUri)
        assertFalse(state.isAnalyzing)
        assertNull(state.result)
        assertNull(state.error)
        assertFalse(state.showCamera)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun `ShowCamera action sets showCamera to true`() = runTest {
        viewModel.onAction(DiseaseAction.ShowCamera)
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.showCamera)
    }

    @Test
    fun `HideCamera action sets showCamera to false`() = runTest {
        viewModel.onAction(DiseaseAction.ShowCamera)
        advanceUntilIdle()
        viewModel.onAction(DiseaseAction.HideCamera)
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.showCamera)
    }

    @Test
    fun `CaptureImage updates state and starts analysis`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.success(mockAnalysis)
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        
        // Should update captured image immediately
        assertEquals(mockUri, viewModel.uiState.value.capturedImageUri)
        assertFalse(viewModel.uiState.value.showCamera)
        
        advanceUntilIdle()
        
        // After analysis completes
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals(mockAnalysis, viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
        
        coVerify { analyzePlantDiseaseUseCase(mockUri) }
    }

    @Test
    fun `successful analysis emits AnalysisComplete event`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.success(mockAnalysis)
        
        // Start collecting events before triggering action
        val events = mutableListOf<DiseaseEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        job.cancel()
        
        // Verify event was emitted
        val analysisEvent = events.filterIsInstance<DiseaseEvent.AnalysisComplete>().firstOrNull()
        assertEquals(mockAnalysis, analysisEvent?.analysis)
    }

    @Test
    fun `failed analysis sets error state`() = runTest {
        val errorMessage = "Analysis failed"
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.failure(Exception(errorMessage))
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `failed analysis emits ShowError event`() = runTest {
        val errorMessage = "Analysis failed"
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.failure(Exception(errorMessage))
        
        // Start collecting events before triggering action
        val events = mutableListOf<DiseaseEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        job.cancel()
        
        // Verify error event was emitted
        val errorEvent = events.filterIsInstance<DiseaseEvent.ShowError>().firstOrNull()
        assertEquals(errorMessage, errorEvent?.message)
    }

    @Test
    fun `RetryAnalysis calls use case again with same URI`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.failure(Exception("First attempt failed"))
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        // Now retry should succeed
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.success(mockAnalysis)
        viewModel.onAction(DiseaseAction.RetryAnalysis)
        advanceUntilIdle()
        
        assertEquals(mockAnalysis, viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
        
        coVerify(exactly = 2) { analyzePlantDiseaseUseCase(mockUri) }
    }

    @Test
    fun `ClearResult resets state`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } returns Result.success(mockAnalysis)
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        viewModel.onAction(DiseaseAction.ClearResult)
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.capturedImageUri)
        assertNull(viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
    }

    // History loading is tested via init block, no public action to test
    
    @Test
    fun `isAnalyzing is true during analysis`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockAnalysis)
        }
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        
        // Check immediately - should be analyzing
        assertTrue(viewModel.uiState.value.isAnalyzing)
        
        advanceUntilIdle()
        
        // After completion - should not be analyzing
        assertFalse(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `exception during analysis is handled gracefully`() = runTest {
        coEvery { analyzePlantDiseaseUseCase(any()) } throws RuntimeException("Unexpected error")
        
        viewModel.onAction(DiseaseAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertTrue(viewModel.uiState.value.error?.isNotEmpty() == true)
        assertNull(viewModel.uiState.value.result)
    }
}
