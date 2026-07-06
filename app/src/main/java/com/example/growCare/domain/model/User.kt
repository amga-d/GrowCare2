package com.example.growCare.domain.model

/**
 * Domain model representing a user in the application
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val profilePictureUrl: String? = null,
    val phoneNumber: String? = null,
    val location: String? = null,
    val farmSize: Double? = null, // in acres
    val preferredCrops: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
