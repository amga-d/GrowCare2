package com.example.growCare.domain.usecase.user

import android.net.Uri
import com.example.growCare.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to upload a profile picture
 * @return Result with download URL of uploaded image
 */
class UploadProfilePictureUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            userRepository.updateProfilePicture(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
