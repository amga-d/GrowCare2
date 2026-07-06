package com.example.growCare.domain.usecase.detection

import com.example.growCare.domain.repository.DetectionRepository
import javax.inject.Inject

class DeleteDiseaseScanUseCase @Inject constructor(
    private val repository: DetectionRepository
) {
    suspend operator fun invoke(analysisId: String): Result<Unit> {
        return repository.deleteDiseaseAnalysis(analysisId)
    }
}
