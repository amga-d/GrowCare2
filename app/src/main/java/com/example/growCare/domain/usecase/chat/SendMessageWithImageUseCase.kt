package com.example.growCare.domain.usecase.chat

import android.net.Uri
import com.example.growCare.domain.model.ChatMessage
import com.example.growCare.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for sending chat messages with image attachments
 */
class SendMessageWithImageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(
        message: String,
        imageUri: Uri,
        conversationId: String = "default"
    ): Flow<ChatMessage> {
        return repository.sendMessageWithImage(message, imageUri, conversationId)
    }
}
