package com.example.growCare.presentation.screens.seed

import android.net.Uri
import com.example.growCare.domain.model.ColorConsistency
import com.example.growCare.domain.model.DamageType
import com.example.growCare.domain.model.SeedQuality
import com.example.growCare.domain.model.SeedSize
import com.example.growCare.domain.usecase.detection.AnalyzeSeedQualityUseCase
import com.example.growCare.domain.usecase.detection.GetSeedHistoryUseCase
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
class SeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: SeedViewModel
    private lateinit var analyzeSeedQualityUseCase: AnalyzeSeedQualityUseCase
    private lateinit var getSeedHistoryUseCase: GetSeedHistoryUseCase
    
    private val mockUri = mockk<Uri>(relaxed = true)
    private val mockSeedQuality = SeedQuality(
        id = "test_seed_id",
        userId = "test_user",
        seedType = "Wheat",
        imageUrl = "https://example.com/seeds.jpg",
        qualityScore = 85,
        sizeAssessment = SeedSize.MEDIUM,
        colorConsistency = ColorConsistency.UNIFORM,
        damagePercentage = 5,
        damageTypes = listOf(DamageType.NONE),
        germinationPotential = 90,
        recommendations = listOf("Store in cool dry place", "Use within 6 months"),
        storageAdvice = "Keep in airtight container",
        isRecommendedForUse = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        analyzeSeedQualityUseCase = mockk()
        getSeedHistoryUseCase = mockk()
        
        // Mock history use case that's called in init
        every { getSeedHistoryUseCase() } returns flowOf(emptyList())
        
        viewModel = SeedViewModel(analyzeSeedQualityUseCase, getSeedHistoryUseCase)
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
        viewModel.onAction(SeedAction.ShowCamera)
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.showCamera)
    }

    @Test
    fun `HideCamera action sets showCamera to false`() = runTest {
        viewModel.onAction(SeedAction.ShowCamera)
        advanceUntilIdle()
        viewModel.onAction(SeedAction.HideCamera)
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.showCamera)
    }

    @Test
    fun `CaptureImage updates state and starts analysis`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(mockSeedQuality)
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        
        assertEquals(mockUri, viewModel.uiState.value.capturedImageUri)
        assertFalse(viewModel.uiState.value.showCamera)
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals(mockSeedQuality, viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
        
        coVerify { analyzeSeedQualityUseCase(mockUri, "Unknown") }
    }

    @Test
    fun `successful analysis emits AnalysisComplete event`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(mockSeedQuality)
        
        // Start collecting events before triggering action
        val events = mutableListOf<SeedEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        job.cancel()
        
        // Verify event was emitted
        val analysisEvent = events.filterIsInstance<SeedEvent.AnalysisComplete>().firstOrNull()
        assertEquals(mockSeedQuality, analysisEvent?.analysis)
    }

    @Test
    fun `failed analysis sets error state`() = runTest {
        val errorMessage = "Seed analysis failed"
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.failure(Exception(errorMessage))
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `failed analysis emits ShowError event`() = runTest {
        val errorMessage = "Seed analysis failed"
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.failure(Exception(errorMessage))
        
        // Start collecting events before triggering action
        val events = mutableListOf<SeedEvent>()
        val job = launch {
            viewModel.events.collect { events.add(it) }
        }
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        job.cancel()
        
        // Verify error event was emitted
        val errorEvent = events.filterIsInstance<SeedEvent.ShowError>().firstOrNull()
        assertEquals(errorMessage, errorEvent?.message)
    }

    @Test
    fun `RetryAnalysis calls use case again with same URI`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.failure(Exception("First attempt failed"))
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(mockSeedQuality)
        viewModel.onAction(SeedAction.RetryAnalysis)
        advanceUntilIdle()
        
        assertEquals(mockSeedQuality, viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
        
        coVerify(exactly = 2) { analyzeSeedQualityUseCase(mockUri, "Unknown") }
    }

    @Test
    fun `ClearResult resets state`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(mockSeedQuality)
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        viewModel.onAction(SeedAction.ClearResult)
        advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.capturedImageUri)
        assertNull(viewModel.uiState.value.result)
        assertNull(viewModel.uiState.value.error)
    }

    // Color tests require Android SDK - use UI tests instead
    // @Test
    // fun `getQualityColor returns correct colors`() {
    //     assertEquals(android.graphics.Color.parseColor("#4CAF50"), viewModel.getQualityColor(85).hashCode())
    //     assertEquals(android.graphics.Color.parseColor("#FFC107"), viewModel.getQualityColor(70).hashCode())
    //     assertEquals(android.graphics.Color.parseColor("#F44336"), viewModel.getQualityColor(45).hashCode())
    // }

    @Test
    fun `getQualityLabel returns correct labels`() {
        assertEquals("Excellent", viewModel.getQualityLabel(92))
        assertEquals("Very Good", viewModel.getQualityLabel(85))
        assertEquals("Good", viewModel.getQualityLabel(75))
        assertEquals("Fair", viewModel.getQualityLabel(65))
        assertEquals("Poor", viewModel.getQualityLabel(55))
        assertEquals("Very Poor", viewModel.getQualityLabel(40))
    }

    // History loading is tested via init block, no public action to test
    
    @Test
    fun `isAnalyzing is true during analysis`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(mockSeedQuality)
        }
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        
        assertTrue(viewModel.uiState.value.isAnalyzing)
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `exception during analysis is handled gracefully`() = runTest {
        coEvery { analyzeSeedQualityUseCase(any(), any()) } throws RuntimeException("Unexpected error")
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertTrue(viewModel.uiState.value.error?.isNotEmpty() == true)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `high quality seed analysis`() = runTest {
        val highQualitySeeds = mockSeedQuality.copy(
            qualityScore = 95,
            damagePercentage = 0,
            germinationPotential = 95,
            isRecommendedForUse = true
        )
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(highQualitySeeds)
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        val result = viewModel.uiState.value.result
        assertTrue(result!!.isRecommendedForUse)
        assertEquals(95, result.qualityScore)
        assertEquals("Excellent", viewModel.getQualityLabel(result.qualityScore))
    }

    @Test
    fun `low quality seed analysis`() = runTest {
        val lowQualitySeeds = mockSeedQuality.copy(
            qualityScore = 45,
            damagePercentage = 30,
            germinationPotential = 40,
            isRecommendedForUse = false
        )
        coEvery { analyzeSeedQualityUseCase(any(), any()) } returns Result.success(lowQualitySeeds)
        
        viewModel.onAction(SeedAction.CaptureImage(mockUri))
        advanceUntilIdle()
        
        val result = viewModel.uiState.value.result
        assertFalse(result!!.isRecommendedForUse)
        assertEquals(45, result.qualityScore)
        assertEquals("Very Poor", viewModel.getQualityLabel(result.qualityScore))
    }
}
