package com.example.growCare.domain.usecase.detection

import com.example.growCare.domain.model.DiseaseAnalysis
import com.example.growCare.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving disease detection history
 */
class GetDiseaseHistoryUseCase @Inject constructor(
    private val repository: DetectionRepository
) {
    operator fun invoke(): Flow<List<DiseaseAnalysis>> {
        return repository.getDiseaseHistory()
    }
}
