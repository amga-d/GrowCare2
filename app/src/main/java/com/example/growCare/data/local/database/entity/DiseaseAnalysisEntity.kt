package com.example.growCare.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.growCare.data.local.database.converter.StringListConverter

/**
 * Room entity for storing disease analysis results locally
 * Maps to the DiseaseAnalysis domain model
 */
@Entity(tableName = "disease_analyses")
@TypeConverters(StringListConverter::class)
data class DiseaseAnalysisEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val cropName: String?,
    val imageUrl: String,
    val diseaseName: String,
    val confidence: Int,
    val symptoms: List<String>,
    val severity: String, // Stored as String, converted from enum
    val treatment: List<String>,
    val prevention: List<String>,
    val additionalNotes: String?,
    val timestamp: Long
)
