package com.example.growCare.data.mapper

import com.example.growCare.data.local.database.entity.DiseaseAnalysisEntity
import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.model.DiseaseSeverity
import javax.inject.Inject

/**
 * Mapper to convert between DiseaseAnalysis domain model and DiseaseAnalysisEntity
 */
class DiseaseAnalysisMapper @Inject constructor() {
    
    /**
     * Convert domain model to entity
     */
    fun toEntity(analysis: DiseaseAnalysis): DiseaseAnalysisEntity {
        return DiseaseAnalysisEntity(
            id = analysis.id,
            userId = analysis.userId,
            cropName = analysis.cropName,
            imageUrl = analysis.imageUrl,
            diseaseName = analysis.diseaseName,
            confidence = analysis.confidence,
            symptoms = analysis.symptoms,
            severity = analysis.severity.name,
            treatment = analysis.treatment,
            prevention = analysis.prevention,
            additionalNotes = analysis.additionalNotes,
            timestamp = analysis.timestamp
        )
    }
    
    /**
     * Convert entity to domain model
     */
    fun toDomain(entity: DiseaseAnalysisEntity): DiseaseAnalysis {
        return DiseaseAnalysis(
            id = entity.id,
            userId = entity.userId,
            cropName = entity.cropName,
            imageUrl = entity.imageUrl,
            diseaseName = entity.diseaseName,
            confidence = entity.confidence,
            symptoms = entity.symptoms,
            severity = DiseaseSeverity.valueOf(entity.severity),
            treatment = entity.treatment,
            prevention = entity.prevention,
            additionalNotes = entity.additionalNotes,
            timestamp = entity.timestamp
        )
    }
    
    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<DiseaseAnalysisEntity>): List<DiseaseAnalysis> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(analyses: List<DiseaseAnalysis>): List<DiseaseAnalysisEntity> {
        return analyses.map { toEntity(it) }
    }
}
