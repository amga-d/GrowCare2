package com.example.growCare.data.local.inference

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Local on-device interface for agricultural assistant responses.
 */
interface LocalChatInference {
    fun streamChatReply(prompt: String): Flow<String>
    suspend fun replyWithImage(prompt: String, imageUri: Uri): String
}
