package com.example.growCare.data.repository

import android.net.Uri
import com.example.growCare.data.local.database.entity.ChatMessageEntity
import com.example.growCare.data.local.datasource.ChatLocalDataSource
import com.example.growCare.data.local.inference.LocalChatInference
import com.example.growCare.domain.model.ChatMessage
import com.example.growCare.domain.repository.ChatRepository
import com.example.growCare.domain.repository.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository
 * Handles AI chat interactions with streaming responses and persistence
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val localChatInference: LocalChatInference,
    private val chatLocalDataSource: ChatLocalDataSource
) : ChatRepository {

    override fun sendMessage(
        message: String,
        conversationId: String
    ): Flow<ChatMessage> = flow {
        try {
            // Step 1: Create and emit user message
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            
            // Save user message
            saveMessage(conversationId, userMessage)
            emit(userMessage)
            
            // Step 2: Stream local on-device response
            val aiMessageId = UUID.randomUUID().toString()
            var finalResponse = ""
            
            localChatInference.streamChatReply(message).collect { fullResponseSoFar ->
                finalResponse = fullResponseSoFar
                
                // Emit streaming message
                emit(ChatMessage(
                    id = aiMessageId,
                    content = finalResponse,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isStreaming = true
                ))
            }
            
            // Step 3: Emit final AI message and save
            val finalAiMessage = ChatMessage(
                id = aiMessageId,
                content = finalResponse,
                isUser = false,
                timestamp = System.currentTimeMillis(),
                isStreaming = false
            )
            
            saveMessage(conversationId, finalAiMessage)
            emit(finalAiMessage)
            
        } catch (e: Exception) {
            // Emit error message
            emit(ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Sorry, I encountered an error: ${e.message}. Please try again.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                isStreaming = false
            ))
        }
    }

    override fun sendMessageWithImage(
        message: String,
        imageUri: Uri,
        conversationId: String
    ): Flow<ChatMessage> = flow {
        try {
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isUser = true,
                timestamp = System.currentTimeMillis(),
                imageUrl = imageUri.toString()
            )
            
            saveMessage(conversationId, userMessage)
            emit(userMessage)
            
            val response = localChatInference.replyWithImage(message, imageUri)

            val aiMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = response,
                isUser = false,
                timestamp = System.currentTimeMillis(),
                isStreaming = false
            )
            
            saveMessage(conversationId, aiMessage)
            emit(aiMessage)
            
        } catch (e: Exception) {
            emit(ChatMessage(
                id = UUID.randomUUID().toString(),
                content = "Sorry, I couldn't analyze the image: ${e.message}",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                isStreaming = false
            ))
        }
    }

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> = flow {
        try {
            chatLocalDataSource.getConversationMessages(conversationId).collect { entities ->
                val messages = entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        content = entity.content,
                        isUser = entity.isUser,
                        timestamp = entity.timestamp,
                        conversationId = entity.conversationId,
                        imageUrl = entity.imageUrl,
                        isStreaming = false
                    )
                }
                
                emit(messages)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getAllConversations(): Flow<List<Conversation>> = flow {
        try {
            chatLocalDataSource.getAllConversations().collect { latestMessages ->
                val conversations = latestMessages.map { entity ->
                    val title = if (entity.conversationId.startsWith("chat_")) {
                        "Chat"
                    } else {
                        entity.conversationId
                    }

                    Conversation(
                        id = entity.conversationId,
                        title = title,
                        lastMessage = entity.content.take(100),
                        lastMessageTime = entity.timestamp,
                        messageCount = chatLocalDataSource.getMessageCount(entity.conversationId)
                    )
                }.sortedByDescending { it.lastMessageTime }

                emit(conversations)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            chatLocalDataSource.deleteConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllHistory(): Result<Unit> {
        return try {
            chatLocalDataSource.clearAllMessages()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMessage(
        conversationId: String,
        message: ChatMessage
    ): Result<Unit> {
        return try {
            chatLocalDataSource.saveMessage(
                ChatMessageEntity(
                    id = message.id,
                    content = message.content,
                    isUser = message.isUser,
                    timestamp = message.timestamp,
                    conversationId = conversationId,
                    imageUrl = message.imageUrl
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
