package com.example.growCare.domain.usecase.user

import com.example.growCare.domain.model.User
import com.example.growCare.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to save/create a new user profile
 */
class SaveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        userRepository.saveUserProfile(user)
    }
}
