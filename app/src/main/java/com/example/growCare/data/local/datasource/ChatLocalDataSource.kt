package com.example.growCare.data.local.datasource

import com.example.growCare.data.local.database.dao.ChatDao
import com.example.growCare.data.local.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for chat messages
 * Handles all database operations for chat messages
 */
@Singleton
class ChatLocalDataSource @Inject constructor(
    private val chatDao: ChatDao
) {
    /**
     * Get all messages for a conversation
     */
    fun getConversationMessages(conversationId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getConversationMessages(conversationId)
    }
    
    /**
     * Get all conversations (latest message from each)
     */
    fun getAllConversations(): Flow<List<ChatMessageEntity>> {
        return chatDao.getAllConversations()
    }
    
    /**
     * Save a chat message
     */
    suspend fun saveMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }
    
    /**
     * Save multiple messages
     */
    suspend fun saveMessages(messages: List<ChatMessageEntity>) {
        chatDao.insertMessages(messages)
    }
    
    /**
     * Delete a conversation
     */
    suspend fun deleteConversation(conversationId: String) {
        chatDao.deleteConversation(conversationId)
    }
    
    /**
     * Delete all messages
     */
    suspend fun clearAllMessages() {
        chatDao.deleteAllMessages()
    }
    
    /**
     * Get message count for a conversation
     */
    suspend fun getMessageCount(conversationId: String): Int {
        return chatDao.getMessageCount(conversationId)
    }
}
