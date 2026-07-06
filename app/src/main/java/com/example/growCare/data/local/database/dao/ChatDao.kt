package com.example.growCare.data.local.database.dao

import androidx.room.*
import com.example.growCare.data.local.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat messages
 * Provides methods for CRUD operations on chat messages
 */
@Dao
interface ChatDao {
    
    /**
     * Get all messages for a specific conversation
     * Returns a Flow for reactive updates
     */
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getConversationMessages(conversationId: String): Flow<List<ChatMessageEntity>>
    
    /**
     * Get all conversations (distinct conversationIds)
     * Returns latest message from each conversation
     */
    @Query("""
        SELECT * FROM chat_messages 
        WHERE id IN (
            SELECT id FROM chat_messages 
            GROUP BY conversationId 
            HAVING timestamp = MAX(timestamp)
        )
        ORDER BY timestamp DESC
    """)
    fun getAllConversations(): Flow<List<ChatMessageEntity>>
    
    /**
     * Insert a single message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    /**
     * Delete all messages in a conversation
     */
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String)
    
    /**
     * Delete a specific message
     */
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
    
    /**
     * Delete all chat messages
     */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
    
    /**
     * Get message count for a conversation
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int
}
