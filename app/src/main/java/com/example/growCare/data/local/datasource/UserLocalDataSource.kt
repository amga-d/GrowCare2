package com.example.growCare.data.local.datasource

import com.example.growCare.data.local.database.dao.UserDao
import com.example.growCare.data.local.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for user data
 * Handles all database operations for user information
 */
@Singleton
class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao
) {
    /**
     * Get user by ID (reactive)
     */
    fun getUserById(userId: String): Flow<UserEntity?> {
        return userDao.getUserById(userId)
    }
    
    /**
     * Get user by ID (one-time)
     */
    suspend fun getUserByIdOnce(userId: String): UserEntity? {
        return userDao.getUserByIdOnce(userId)
    }
    
    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }
    
    /**
     * Save or update user
     */
    suspend fun saveUser(user: UserEntity) {
        userDao.insertUser(user)
    }
    
    /**
     * Update user
     */
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }
    
    /**
     * Delete user
     */
    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }
    
    /**
     * Delete user by ID
     */
    suspend fun deleteUserById(userId: String) {
        userDao.deleteUserById(userId)
    }
    
    /**
     * Check if user exists
     */
    suspend fun userExists(userId: String): Boolean {
        return userDao.userExists(userId)
    }
    
    /**
     * Get all users
     */
    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }
}
