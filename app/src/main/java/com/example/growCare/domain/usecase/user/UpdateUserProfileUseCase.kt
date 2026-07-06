package com.example.growCare.domain.usecase.user

import com.example.growCare.domain.model.User
import com.example.growCare.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to update user profile information
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userRepository.updateProfile(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
