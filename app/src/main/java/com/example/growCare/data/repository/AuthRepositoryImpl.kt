package com.example.growCare.data.repository

import com.example.growCare.domain.model.User
import com.example.growCare.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 * Handles authentication operations using a local dummy implementation for offline-first architecture
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private var currentUser: User? = null

    /**
     * Sign in with email and password
     */
    override suspend fun signIn(email: String, password: String): Result<User> {
        val user = User(
            uid = UUID.randomUUID().toString(),
            email = email,
            displayName = "Local User"
        )
        currentUser = user
        return Result.success(user)
    }

    /**
     * Create new account with email and password
     */
    override suspend fun signUp(email: String, password: String, displayName: String?): Result<User> {
        val user = User(
            uid = UUID.randomUUID().toString(),
            email = email,
            displayName = displayName ?: "Local User"
        )
        currentUser = user
        return Result.success(user)
    }

    /**
     * Sign out current user
     */
    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }

    /**
     * Get current authenticated user
     */
    override fun getCurrentUser(): User? {
        return currentUser
    }

    /**
     * Check if user is authenticated
     */
    override fun isAuthenticated(): Boolean {
        return currentUser != null
    }

    /**
     * Observe authentication state changes
     */
    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        trySend(isAuthenticated())
        awaitClose { }
    }

    /**
     * Send password reset email
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Update user display name
     */
    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        currentUser = currentUser?.copy(displayName = displayName)
        return Result.success(Unit)
    }

    /**
     * Delete user account
     */
    override suspend fun deleteAccount(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
}

