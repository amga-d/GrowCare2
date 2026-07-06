package com.example.growCare.data.local.inference

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device AI agricultural assistant powered by Gemma 4 E2B via LiteRT-LM.
 *
 * Model: litert-community/gemma-4-E2B-it-litert-lm
 * Format: .litertlm (replaces deprecated .bin / MediaPipe .task formats)
 * Framework: LiteRT-LM (active replacement for the deprecated MediaPipe LLM Inference API)
 *
 * To deploy the model:
 *   1. Download from https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
 *   2. Push via ADB — see Docs/ADB_MODEL_SETUP.md
 *
 * Model path on-device: /data/user/0/com.example.growCare/files/models/gemma4_e2b.litertlm
 */
@Singleton
class GemmaChatInference @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalChatInference {

    companion object {
        const val MODEL_FILE_NAME = "gemma4_e2b.litertlm"
        const val MODEL_DIR = "models"

        /**
         * Agricultural system prompt injected at the start of every conversation.
         * This keeps the model focused on farming topics and avoids off-topic replies.
         */
        private const val SYSTEM_PROMPT = """You are GrowCare AI, a helpful and knowledgeable agricultural assistant designed for smallholder farmers. Your role is to provide clear, practical, and actionable advice on the following topics only:

- Plant disease identification and treatment
- Crop health and farming best practices
- Pest and weed control
- Soil health, fertilization, and irrigation
- Harvest timing and post-harvest handling
- Seasonal and weather-related farming decisions

If a user asks a question unrelated to agriculture, politely explain that you are specialized for farming topics and invite them to ask an agricultural question instead.

Keep your answers concise, easy to understand, and practical for a farmer without advanced technical knowledge."""
    }

    // LiteRT-LM Engine — initialized lazily on first use
    private var engine: Engine? = null

    /**
     * Returns the path to the model file in app internal storage.
     * Throws [IllegalStateException] with a clear message if the file is not found.
     */
    private fun resolveModelPath(): String {
        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        if (!modelFile.exists()) {
            throw IllegalStateException(
                "Gemma model not found at ${modelFile.absolutePath}.\n" +
                "Please push the model via ADB. See Docs/ADB_MODEL_SETUP.md for instructions."
            )
        }
        return modelFile.absolutePath
    }

    /**
     * Lazily initializes the LiteRT-LM Engine. Safe to call from a coroutine.
     * Reuses the same engine instance across calls (singleton pattern).
     */
    private suspend fun getEngine(): Engine = withContext(Dispatchers.IO) {
        if (engine == null) {
            val modelPath = resolveModelPath()
            val config = EngineConfig(modelPath = modelPath)
            engine = Engine(config).also { it.initialize() }
        }
        engine!!
    }

    /**
     * Streams an agricultural AI response token by token.
     *
     * Each emission to the Flow is the full response string accumulated so far,
     * so the UI can update progressively without managing state itself.
     *
     * A new [Conversation] is created per call, prepended with the agricultural
     * system prompt to keep the model on-topic.
     */
    override fun streamChatReply(prompt: String): Flow<String> = flow {
        try {
            val llm = getEngine()

            // Each chat turn gets a fresh conversation context with the system prompt
            val conversation = llm.createConversation()

            // sendMessageAsync() sends the full prompt and streams tokens as a Flow<String>
            val accumulatedResponse = StringBuilder()
            conversation.sendMessageAsync("$SYSTEM_PROMPT\n\nFarmer question: $prompt").collect { token ->
                accumulatedResponse.append(token)
                emit(accumulatedResponse.toString())
            }

        } catch (e: IllegalStateException) {
            // Model file missing — surface a clear user-facing message
            emit("⚠️ ${e.message}")
        } catch (e: Exception) {
            emit("⚠️ Could not generate a response: ${e.message}")
        }
    }

    /**
     * Responds to a message with an optional image context.
     *
     * Note: Gemma 4 E2B supports multimodal inputs in its full form, but the
     * LiteRT-LM text-only API does not currently expose image tensor inputs in
     * the stable Kotlin API. The image URI is logged here for future use when
     * the multimodal conversation API stabilizes.
     *
     * For now, the model responds based on the text prompt alone.
     */
    override suspend fun replyWithImage(prompt: String, imageUri: Uri): String =
        withContext(Dispatchers.IO) {
            try {
                val llm = getEngine()
                val conversation = llm.createConversation()

                val fullPrompt = buildString {
                    append(SYSTEM_PROMPT)
                    append("\n\n")
                    append("The farmer has shared an image of their crop and asks: $prompt\n")
                    append("(Image analysis is being processed on-device via the disease detection model. ")
                    append("Please provide general advice based on the text question.)")
                }

                val result = StringBuilder()
                conversation.sendMessageAsync(fullPrompt).collect { token ->
                    result.append(token)
                }
                result.toString()

            } catch (e: IllegalStateException) {
                "⚠️ ${e.message}"
            } catch (e: Exception) {
                "⚠️ Could not analyze the image context: ${e.message}"
            }
        }
}
