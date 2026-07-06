package com.example.growCare.domain.model

/**
 * Domain model representing plant disease analysis results
 */
data class DiseaseAnalysis(
    val id: String,
    val userId: String,
    val cropName: String? = null,
    val imageUrl: String,
    val diseaseName: String,
    val confidence: Int, // 0-100
    val symptoms: List<String>,
    val severity: DiseaseSeverity,
    val treatment: List<String>,
    val prevention: List<String>,
    val additionalNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DiseaseSeverity {
    MILD,
    MODERATE,
    SEVERE
}

/**
 * Helper function to determine severity from confidence and disease name
 */
fun DiseaseSeverity.toDisplayString(): String {
    return when (this) {
        DiseaseSeverity.MILD -> "Mild"
        DiseaseSeverity.MODERATE -> "Moderate"
        DiseaseSeverity.SEVERE -> "Severe"
    }
}
