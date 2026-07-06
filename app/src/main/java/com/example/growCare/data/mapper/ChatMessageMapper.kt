package com.example.growCare.data.mapper

import com.example.growCare.data.local.database.entity.ChatMessageEntity
import com.example.growCare.domain.model.ChatMessage
import javax.inject.Inject

/**
 * Mapper to convert between ChatMessage domain model and ChatMessageEntity
 */
class ChatMessageMapper @Inject constructor() {
    
    /**
     * Convert domain model to entity
     */
    fun toEntity(message: ChatMessage): ChatMessageEntity {
        return ChatMessageEntity(
            id = message.id,
            content = message.content,
            isUser = message.isUser,
            timestamp = message.timestamp,
            conversationId = message.conversationId ?: "default"
        )
    }
    
    /**
     * Convert entity to domain model
     */
    fun toDomain(entity: ChatMessageEntity): ChatMessage {
        return ChatMessage(
            id = entity.id,
            content = entity.content,
            isUser = entity.isUser,
            timestamp = entity.timestamp,
            isStreaming = false,
            conversationId = entity.conversationId
        )
    }
    
    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<ChatMessageEntity>): List<ChatMessage> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(messages: List<ChatMessage>): List<ChatMessageEntity> {
        return messages.map { toEntity(it) }
    }
}
