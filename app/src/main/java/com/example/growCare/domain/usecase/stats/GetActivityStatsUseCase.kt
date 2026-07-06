package com.example.growCare.domain.usecase.stats

import com.example.growCare.domain.model.ActivityStats
import com.example.growCare.domain.repository.ChatRepository
import com.example.growCare.domain.repository.DetectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case for retrieving activity statistics
 */
class GetActivityStatsUseCase @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<ActivityStats> {
        return combine(
            detectionRepository.getDiseaseHistory(),
            chatRepository.getAllConversations()
        ) { diseaseHistory, conversations ->
            ActivityStats(
                totalScans = diseaseHistory.size,
                totalChats = conversations.size,
                diseaseScans = diseaseHistory.size
            )
        }
    }
}
