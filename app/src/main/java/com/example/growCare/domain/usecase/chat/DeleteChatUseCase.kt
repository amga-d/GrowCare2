package com.example.growCare.domain.usecase.chat

import com.example.growCare.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return repository.deleteConversation(conversationId)
    }
}
