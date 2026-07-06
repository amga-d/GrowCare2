package com.example.growCare.domain.usecase.chat

import com.example.growCare.domain.model.ChatMessage
import com.example.growCare.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving chat history
 */
class GetChatHistoryUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(conversationId: String = "default"): Flow<List<ChatMessage>> {
        return repository.getChatHistory(conversationId)
    }
}
