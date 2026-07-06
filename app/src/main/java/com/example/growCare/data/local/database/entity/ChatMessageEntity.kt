package com.example.growCare.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing chat messages locally
 * Maps to the ChatMessage domain model
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val conversationId: String,
    val imageUrl: String? = null
)
