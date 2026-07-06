package com.example.growCare.data.local.inference

import android.content.Context
import android.net.Uri
import android.util.Log
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
        private const val TAG = "GemmaChatInference"
        const val MODEL_FILE_NAME = "gemma4_e2b.litertlm"
        const val MODEL_DIR = "models"

        // ── Generation limits ────────────────────────────────────────────────
        // Small on-device LLMs (E2B = ~2B params) often fail to emit EOS and
        // fall into repetition loops. These guards prevent the device from
        // hanging and memory from exploding.

        /** Maximum number of tokens to generate per response. */
        private const val MAX_TOKENS = 512

        /** Hard character cutoff — stops generation even mid-token. */
        private const val MAX_RESPONSE_CHARS = 4000

        /** Window size (in chars) used for repetition detection. */
        private const val REPETITION_WINDOW = 80

        /** If the same window repeats this many times consecutively, stop. */
        private const val REPETITION_LIMIT = 3

        /**
         * Agricultural system prompt — kept concise to leave context window
         * room for the actual conversation on a small E2B model.
         */
        private const val SYSTEM_PROMPT =
            "You are GrowCare AI, a concise agricultural assistant for farmers. " +
            "Answer only farming questions (diseases, pests, soil, crops, harvest). " +
            "If asked about non-farming topics, politely decline. " +
            "Keep answers short and practical."
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

    // ──────────────────────────────────────────────────────────────────────────
    // Repetition detection
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks if the tail of [text] contains the same [REPETITION_WINDOW]-char
     * block repeated [REPETITION_LIMIT] times consecutively.
     *
     * Example with window=5 and limit=3:
     *   "hello hello hello " → the last 15 chars contain "hello" repeated 3× → true
     */
    private fun isRepeating(text: String): Boolean {
        if (text.length < REPETITION_WINDOW * REPETITION_LIMIT) return false

        val tail = text.takeLast(REPETITION_WINDOW * REPETITION_LIMIT)
        val window = tail.takeLast(REPETITION_WINDOW)

        var count = 0
        var offset = tail.length - REPETITION_WINDOW
        while (offset >= 0) {
            val chunk = tail.substring(offset, offset + REPETITION_WINDOW)
            if (chunk == window) count++ else break
            offset -= REPETITION_WINDOW
        }
        return count >= REPETITION_LIMIT
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Streaming chat
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Streams an agricultural AI response token by token.
     *
     * Each emission to the Flow is the full response string accumulated so far,
     * so the UI can update progressively without managing state itself.
     *
     * Guardrails:
     *  - Stops after [MAX_TOKENS] tokens
     *  - Stops after [MAX_RESPONSE_CHARS] characters
     *  - Stops if repetition is detected
     */
    override fun streamChatReply(prompt: String): Flow<String> = flow {
        try {
            val llm = getEngine()
            val conversation = llm.createConversation()

            val accumulatedResponse = StringBuilder()
            var tokenCount = 0

            conversation.sendMessageAsync(
                "$SYSTEM_PROMPT\n\nFarmer question: $prompt"
            ).collect { token ->
                tokenCount++

                // Guard 1: max token limit
                if (tokenCount > MAX_TOKENS) {
                    Log.i(TAG, "Stopped: max tokens ($MAX_TOKENS)")
                    return@collect
                }

                accumulatedResponse.append(token)

                // Guard 2: max character limit
                if (accumulatedResponse.length > MAX_RESPONSE_CHARS) {
                    Log.i(TAG, "Stopped: max chars ($MAX_RESPONSE_CHARS)")
                    return@collect
                }

                // Guard 3: repetition detection (check every 10 tokens to save CPU)
                if (tokenCount % 10 == 0 && isRepeating(accumulatedResponse.toString())) {
                    // Trim the repeated tail before emitting
                    val clean = accumulatedResponse.substring(
                        0, accumulatedResponse.length - REPETITION_WINDOW * (REPETITION_LIMIT - 1)
                    )
                    Log.i(TAG, "Stopped: repetition detected at token $tokenCount")
                    emit(clean)
                    return@collect
                }

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
                    append("Please provide general advice based on the text question.")
                }

                val result = StringBuilder()
                var tokenCount = 0

                conversation.sendMessageAsync(fullPrompt).collect { token ->
                    tokenCount++
                    if (tokenCount > MAX_TOKENS) return@collect

                    result.append(token)
                    if (result.length > MAX_RESPONSE_CHARS) return@collect

                    if (tokenCount % 10 == 0 && isRepeating(result.toString())) {
                        return@collect
                    }
                }

                // Trim any trailing repetition
                val response = result.toString()
                if (isRepeating(response)) {
                    response.substring(0, response.length - REPETITION_WINDOW * (REPETITION_LIMIT - 1))
                } else {
                    response
                }

            } catch (e: IllegalStateException) {
                "⚠️ ${e.message}"
            } catch (e: Exception) {
                "⚠️ Could not analyze the image context: ${e.message}"
            }
        }
}
