package com.example.growCare.domain.model

/**
 * Activity statistics for user profile
 */
data class ActivityStats(
    val totalScans: Int = 0,
    val totalChats: Int = 0,
    val diseaseScans: Int = 0
)
