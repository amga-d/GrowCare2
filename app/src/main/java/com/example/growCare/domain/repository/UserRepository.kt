package com.example.growCare.domain.repository

import android.net.Uri
import com.example.growCare.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile management
 * Handles user data and preferences
 */
interface UserRepository {
    
    /**
     * Get current user profile
     * @return Flow of User, updates when profile changes
     */
    fun getUserProfile(): Flow<User?>
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(user: User): Result<Unit>
    
    /**
     * Update profile picture
     * @param imageUri Local URI of the new profile picture
     * @return Result with download URL of uploaded image
     */
    suspend fun updateProfilePicture(imageUri: Uri): Result<String>
    
    /**
     * Update display name
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    
    /**
     * Update phone number
     */
    suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit>
    
    /**
     * Update location
     */
    suspend fun updateLocation(location: String): Result<Unit>
    
    /**
     * Update farm size
     */
    suspend fun updateFarmSize(farmSize: Double): Result<Unit>
    
    /**
     * Update preferred crops list
     */
    suspend fun updatePreferredCrops(crops: List<String>): Result<Unit>
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User?>
    
    /**
     * Save new user profile (called after registration)
     */
    suspend fun saveUserProfile(user: User): Result<Unit>
    
    /**
     * Delete user profile and all associated data
     */
    suspend fun deleteUserData(): Result<Unit>
    
    /**
     * Check if user profile exists in Firestore
     */
    suspend fun profileExists(userId: String): Boolean
}
