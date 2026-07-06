package com.example.growCare.domain.usecase.user

import com.example.growCare.domain.model.User
import com.example.growCare.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get the current user's profile
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<User?> {
        return userRepository.getUserProfile()
    }
}
