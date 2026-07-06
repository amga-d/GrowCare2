package com.example.growCare.domain.repository

import com.example.growCare.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 * Abstracts authentication logic from the presentation layer
 */
interface AuthRepository {
    
    /**
     * Sign in with email and password
     * @return Result with User if successful
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Create new account with email and password
     * @return Result with User if successful
     */
    suspend fun signUp(email: String, password: String, displayName: String?): Result<User>
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Get current authenticated user
     * @return User if logged in, null otherwise
     */
    fun getCurrentUser(): User?
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean
    
    /**
     * Observe authentication state changes
     * Emits true when user is logged in, false when logged out
     */
    fun observeAuthState(): Flow<Boolean>
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Update user display name
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    
    /**
     * Delete user account and all associated data
     */
    suspend fun deleteAccount(): Result<Unit>
}
