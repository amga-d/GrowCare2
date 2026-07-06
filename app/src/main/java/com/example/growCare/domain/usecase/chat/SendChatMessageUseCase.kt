package com.example.growCare.domain.usecase.chat

import com.example.growCare.domain.model.ChatMessage
import com.example.growCare.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for sending chat messages to AI assistant
 */
class SendChatMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(
        message: String,
        conversationId: String = "default"
    ): Flow<ChatMessage> {
        return repository.sendMessage(message, conversationId)
    }
}
