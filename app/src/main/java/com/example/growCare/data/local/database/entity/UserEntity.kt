package com.example.growCare.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.growCare.data.local.database.converter.StringListConverter

/**
 * Room entity for storing user data locally
 * Maps to the User domain model
 */
@Entity(tableName = "users")
@TypeConverters(StringListConverter::class)
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String?,
    val profilePictureUrl: String?,
    val phoneNumber: String?,
    val location: String?,
    val farmSize: Double?,
    val preferredCrops: List<String>,
    val createdAt: Long,
    val lastLoginAt: Long,
    val lastUpdated: Long
)
