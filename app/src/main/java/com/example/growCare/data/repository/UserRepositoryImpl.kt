package com.example.growCare.data.repository

import android.net.Uri
import com.example.growCare.data.local.database.dao.UserDao
import com.example.growCare.data.mapper.UserMapper
import com.example.growCare.domain.model.User
import com.example.growCare.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of UserRepository
 * Handles user profile data using local Room database for offline-first architecture
 */
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userMapper: UserMapper
) : UserRepository {

    private val LOCAL_USER_ID = "local_user_id"

    private suspend fun getOrCreateUser(): User {
        val userEntity = userDao.getUserByIdOnce(LOCAL_USER_ID)
        return if (userEntity != null) {
            userMapper.toDomain(userEntity)
        } else {
            val newUser = User(uid = LOCAL_USER_ID, email = "offline@growcare.local", displayName = "Local User")
            userDao.insertUser(userMapper.toEntity(newUser))
            newUser
        }
    }

    override fun getUserProfile(): Flow<User?> {
        return userDao.getUserById(LOCAL_USER_ID).map { userEntity ->
            userEntity?.let { userMapper.toDomain(it) } ?: User(uid = LOCAL_USER_ID, email = "offline@growcare.local", displayName = "Local User")
        }
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        return try {
            val userToSave = user.copy(uid = LOCAL_USER_ID)
            userDao.insertUser(userMapper.toEntity(userToSave))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePicture(imageUri: Uri): Result<String> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(profilePictureUrl = imageUri.toString())
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(imageUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(displayName = displayName)
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhoneNumber(phoneNumber: String): Result<Unit> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(phoneNumber = phoneNumber)
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(location: String): Result<Unit> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(location = location)
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFarmSize(farmSize: Double): Result<Unit> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(farmSize = farmSize)
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePreferredCrops(crops: List<String>): Result<Unit> {
        return try {
            val user = getOrCreateUser()
            val updatedUser = user.copy(preferredCrops = crops)
            userDao.insertUser(userMapper.toEntity(updatedUser))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val userEntity = userDao.getUserByIdOnce(LOCAL_USER_ID)
            Result.success(userEntity?.let { userMapper.toDomain(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            userDao.insertUser(userMapper.toEntity(user.copy(uid = LOCAL_USER_ID)))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUserData(): Result<Unit> {
        return try {
            userDao.deleteUserById(LOCAL_USER_ID)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun profileExists(userId: String): Boolean {
        return try {
            userDao.userExists(LOCAL_USER_ID)
        } catch (e: Exception) {
            false
        }
    }
}
