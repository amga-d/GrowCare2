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
        private const val REPETITION_WINDOW = 40

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

    /**
     * Custom exception thrown to forcefully break out of Flow.collect().
     *
     * In Kotlin, `return@collect` only skips the current lambda invocation —
     * it does NOT stop the upstream flow from emitting more tokens.
     * The only way to abort a flow collection is to throw from inside collect().
     * We use a custom exception (not CancellationException) so it doesn't
     * cancel the outer coroutine scope.
     */
    private class StopGenerationException(val reason: String) : Exception(reason)

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
     */
    private fun isRepeating(text: String): Boolean {
        val minLen = REPETITION_WINDOW * REPETITION_LIMIT
        if (text.length < minLen) return false

        val tail = text.takeLast(minLen)
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

    /**
     * Trims trailing repeated blocks from the response.
     */
    private fun trimRepetition(text: String): String {
        if (!isRepeating(text)) return text
        // Find where repetition starts by walking backwards
        val window = text.takeLast(REPETITION_WINDOW)
        var cutPoint = text.length - REPETITION_WINDOW
        while (cutPoint >= REPETITION_WINDOW) {
            val prev = text.substring(cutPoint - REPETITION_WINDOW, cutPoint)
            if (prev == window) {
                cutPoint -= REPETITION_WINDOW
            } else {
                break
            }
        }
        // Keep one instance of the repeated block
        return text.substring(0, cutPoint + REPETITION_WINDOW).trimEnd()
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
     *
     * Uses [StopGenerationException] to forcefully break out of Flow.collect(),
     * since `return@collect` does NOT stop the upstream flow.
     */
    override fun streamChatReply(prompt: String): Flow<String> = flow {
        try {
            val llm = getEngine()
            val conversation = llm.createConversation()

            val accumulated = StringBuilder()
            var tokenCount = 0

            try {
                conversation.sendMessageAsync(
                    "$SYSTEM_PROMPT\n\nFarmer question: $prompt"
                ).collect { token ->
                    tokenCount++
                    accumulated.append(token)

                    // Guard 1: max token limit
                    if (tokenCount > MAX_TOKENS) {
                        throw StopGenerationException("max tokens ($MAX_TOKENS)")
                    }

                    // Guard 2: max character limit
                    if (accumulated.length > MAX_RESPONSE_CHARS) {
                        throw StopGenerationException("max chars ($MAX_RESPONSE_CHARS)")
                    }

                    // Guard 3: repetition detection (check every 5 tokens)
                    if (tokenCount % 5 == 0 && isRepeating(accumulated.toString())) {
                        throw StopGenerationException("repetition at token $tokenCount")
                    }

                    emit(accumulated.toString())
                }
            } catch (e: StopGenerationException) {
                // Expected — generation was intentionally stopped by a guard
                Log.i(TAG, "Generation stopped: ${e.reason}")
            }

            // Final emit with cleaned-up text (trim any trailing repetition)
            val finalText = trimRepetition(accumulated.toString()).trimEnd()
            if (finalText.isNotEmpty()) {
                emit(finalText)
            }

        } catch (e: IllegalStateException) {
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
     * the stable Kotlin API.
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

                try {
                    conversation.sendMessageAsync(fullPrompt).collect { token ->
                        tokenCount++
                        result.append(token)

                        if (tokenCount > MAX_TOKENS) {
                            throw StopGenerationException("max tokens")
                        }
                        if (result.length > MAX_RESPONSE_CHARS) {
                            throw StopGenerationException("max chars")
                        }
                        if (tokenCount % 5 == 0 && isRepeating(result.toString())) {
                            throw StopGenerationException("repetition")
                        }
                    }
                } catch (e: StopGenerationException) {
                    Log.i(TAG, "Generation stopped (image): ${e.reason}")
                }

                trimRepetition(result.toString()).trimEnd()

            } catch (e: IllegalStateException) {
                "⚠️ ${e.message}"
            } catch (e: Exception) {
                "⚠️ Could not analyze the image context: ${e.message}"
            }
        }
}
