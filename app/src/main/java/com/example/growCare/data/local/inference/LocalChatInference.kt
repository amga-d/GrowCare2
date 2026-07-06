package com.example.growCare.data.local.inference

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Local on-device interface for agricultural assistant responses.
 */
interface LocalChatInference {
    fun streamChatReply(prompt: String, history: List<com.example.growCare.domain.model.ChatMessage> = emptyList()): Flow<String>
    suspend fun generateDiseaseAdvice(diseaseName: String): String
}
