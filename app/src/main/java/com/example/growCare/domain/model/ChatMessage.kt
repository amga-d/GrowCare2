package com.example.growCare.domain.model

/**
 * Domain model representing a chat message in the AI assistant conversation
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val conversationId: String? = null,
    val imageUrl: String? = null
)
