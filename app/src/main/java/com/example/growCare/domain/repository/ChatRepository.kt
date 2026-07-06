package com.example.growCare.domain.repository

import android.net.Uri
import com.example.growCare.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations
 * Handles AI chat interactions and message persistence
 */
interface ChatRepository {
    
    /**
     * Send a text message to AI and get response
     * @return Flow emitting chat messages (user message + AI response chunks)
     */
    fun sendMessage(
        message: String,
        conversationId: String = "default"
    ): Flow<ChatMessage>
    
    /**
     * Send message with image attachment
     * @return Flow emitting chat messages with image analysis
     */
    fun sendMessageWithImage(
        message: String,
        imageUri: Uri,
        conversationId: String = "default"
    ): Flow<ChatMessage>
    
    /**
     * Get chat history for a conversation
     * @return Flow of message lists, updates in real-time
     */
    fun getChatHistory(conversationId: String = "default"): Flow<List<ChatMessage>>
    
    /**
     * Get all conversations for the current user
     * @return Flow of conversation IDs with last message preview
     */
    fun getAllConversations(): Flow<List<Conversation>>
    
    /**
     * Delete a specific conversation
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    
    /**
     * Delete all chat history
     */
    suspend fun deleteAllHistory(): Result<Unit>
    
    /**
     * Save a single message to history
     */
    suspend fun saveMessage(conversationId: String, message: ChatMessage): Result<Unit>
}

/**
 * Represents a conversation summary
 */
data class Conversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val messageCount: Int
)
