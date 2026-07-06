package com.example.growCare.domain.usecase.chat

import com.example.growCare.domain.repository.ChatRepository
import com.example.growCare.domain.repository.Conversation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get all chat conversations for the current user
 */
class GetAllConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return chatRepository.getAllConversations()
    }
}
