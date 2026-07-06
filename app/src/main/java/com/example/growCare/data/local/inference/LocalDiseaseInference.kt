package com.example.growCare.data.local.inference

import android.net.Uri

data class LocalDiseaseResult(
    val diseaseName: String,
    val confidence: Int,
    val symptoms: List<String>,
    val treatment: List<String>,
    val prevention: List<String>,
    val additionalNotes: String? = null
)

/**
 * Local interface for on-device disease analysis (YOLO + local LLM pipeline).
 */
interface LocalDiseaseInference {
    suspend fun detectDisease(imageUri: Uri, cropName: String?): LocalDiseaseResult
}
