package com.example.growCare.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.growCare.data.local.database.converter.StringListConverter

/**
 * Room entity for storing AI-generated agricultural tips
 * Implements caching to avoid regenerating tips on every app restart
 */
@Entity(tableName = "ai_tips")
data class AITipEntity(
    @PrimaryKey
    val id: String, // Will be based on weather conditions hash
    val title: String,
    val description: String,
    val weatherConditions: String, // Serialized weather conditions that triggered this tip
    val timestamp: Long, // When the tip was generated
    val expiresAt: Long // When the tip should be regenerated (24 hours from generation)
)
