package com.example.growCare.data.local.database.dao

import androidx.room.*
import com.example.growCare.data.local.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user data
 * Provides methods for CRUD operations on user records
 */
@Dao
interface UserDao {
    
    /**
     * Get user by ID
     * Returns a Flow for reactive updates
     */
    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>
    
    /**
     * Get user by ID (one-time query)
     */
    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserByIdOnce(userId: String): UserEntity?
    
    /**
     * Get user by email
     */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    /**
     * Insert or update a user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    /**
     * Update a user
     */
    @Update
    suspend fun updateUser(user: UserEntity)
    
    /**
     * Delete a user
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    /**
     * Delete user by ID
     */
    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUserById(userId: String)
    
    /**
     * Check if user exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE uid = :userId)")
    suspend fun userExists(userId: String): Boolean
    
    /**
     * Get all users (for admin purposes, rarely used)
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}
