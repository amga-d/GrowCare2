package com.example.growCare.data.local.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.ai.edge.litert.Interpreter
import com.google.ai.edge.litert.gpu.GpuDelegate
import com.google.ai.edge.litert.support.common.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * On-device plant disease detector powered by YOLO11n via LiteRT.
 *
 * Model:   disease_detection.tflite (bundled in assets)
 * Format:  TFLite FP16 — exported with `model.export(format='tflite', half=True)`
 * Task:    Detection (not classification)
 * Classes: 27  (curated PlantVillage — index 0 = Healthy, 1-26 = diseases)
 *
 * I/O:
 *   Input  → [1, 640, 640, 3]  float32, normalized 0-1, letterboxed
 *   Output → [1, 31, 8400]     31 = 4 box coords (xywh) + 27 class scores
 *
 * Post-processing:
 *   1. Transpose output to [8400, 31]
 *   2. Filter anchors below confidence threshold
 *   3. Decode normalized xywh → pixel x1y1x2y2
 *   4. Non-Maximum Suppression (IoU ≥ 0.45)
 *   5. Return top detection mapped to a [LocalDiseaseResult]
 */
@Singleton
class YoloDiseaseInference @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalDiseaseInference {

    companion object {
        private const val TAG = "YoloDiseaseInference"
        private const val MODEL_ASSET = "disease_detection.tflite"
        private const val INPUT_SIZE = 640
        private const val NUM_CLASSES = 27
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val IOU_THRESHOLD = 0.45f

        /**
         * Exact class names from curated_data.yaml — index MUST match training order.
         * Class 0 = Healthy (merged from all crop-specific healthy classes during curation).
         */
        val CLASS_NAMES = listOf(
            "Healthy",
            "Apple - Apple Scab",
            "Apple - Black Rot",
            "Apple - Cedar Apple Rust",
            "Cherry - Powdery Mildew",
            "Corn - Cercospora Leaf Spot / Gray Leaf Spot",
            "Corn - Common Rust",
            "Corn - Northern Leaf Blight",
            "Grape - Black Rot",
            "Grape - Esca (Black Measles)",
            "Grape - Leaf Blight (Isariopsis)",
            "Orange - Huanglongbing (Citrus Greening)",
            "Peach - Bacterial Spot",
            "Pepper - Bacterial Spot",
            "Potato - Early Blight",
            "Potato - Late Blight",
            "Squash - Powdery Mildew",
            "Strawberry - Leaf Scorch",
            "Tomato - Bacterial Spot",
            "Tomato - Early Blight",
            "Tomato - Late Blight",
            "Tomato - Leaf Mold",
            "Tomato - Septoria Leaf Spot",
            "Tomato - Spider Mites",
            "Tomato - Target Spot",
            "Tomato - Yellow Leaf Curl Virus",
            "Tomato - Mosaic Virus"
        )

        /** Static agronomic knowledge keyed by class index. */
        private val DISEASE_INFO = mapOf(
            0 to Triple(
                listOf("No visible disease symptoms detected."),
                listOf("Maintain current crop management practices."),
                listOf("Continue regular scouting to catch early signs of stress.")
            ),
            1 to Triple(
                listOf("Olive-green to brown velvety lesions on leaves and fruit.", "Distorted shoots and leaves."),
                listOf("Apply fungicides (captan, myclobutanil).", "Remove and destroy infected plant material."),
                listOf("Plant resistant apple varieties.", "Ensure good air circulation.")
            ),
            2 to Triple(
                listOf("Small, purple-bordered leaf spots with tan centres.", "Shrivelled, mummified fruit."),
                listOf("Apply copper-based fungicides.", "Prune and remove infected wood."),
                listOf("Avoid wounding trees.", "Maintain tree vigor through balanced fertilization.")
            ),
            3 to Triple(
                listOf("Orange rust-colored spots on upper leaf surface.", "Tube-like spore structures underneath."),
                listOf("Apply myclobutanil or triadimefon fungicides.", "Remove heavily infected branches."),
                listOf("Do not plant apples near eastern red cedars.", "Use resistant varieties where available.")
            ),
            4 to Triple(
                listOf("White powdery fungal growth on young leaves.", "Leaf distortion and premature drop."),
                listOf("Apply sulfur or potassium bicarbonate sprays.", "Remove infected shoots."),
                listOf("Improve canopy airflow with pruning.", "Avoid excess nitrogen fertilization.")
            ),
            5 to Triple(
                listOf("Small grey or tan rectangular leaf spots.", "Lesions bordered by leaf veins."),
                listOf("Apply strobilurin or triazole fungicides.", "Rotate with non-host crops."),
                listOf("Plant resistant hybrids.", "Avoid planting in fields with previous history.")
            ),
            6 to Triple(
                listOf("Pustules of orange-red spores on undersides of leaves.", "Yellow-orange discoloration."),
                listOf("Apply triazole or strobilurin fungicides.", "Scout fields regularly after tasseling."),
                listOf("Plant resistant hybrids.", "Monitor weather forecasts for high-risk conditions.")
            ),
            7 to Triple(
                listOf("Large, elliptical lesions with wavy margins.", "Greyish-green to tan lesions on leaves."),
                listOf("Apply azoxystrobin or propiconazole.", "Plant early to avoid peak infection periods."),
                listOf("Select resistant hybrids.", "Avoid susceptible hybrids in high-risk areas.")
            ),
            8 to Triple(
                listOf("Small, angular reddish-brown lesions on leaves.", "Black shrivelled berries."),
                listOf("Apply copper or myclobutanil fungicides from bud break.", "Remove mummified berries."),
                listOf("Prune for good canopy airflow.", "Remove all infected material after harvest.")
            ),
            9 to Triple(
                listOf("Reddish-brown streaks on canes.", "Interveinal yellowing; leaves wilt and dry."),
                listOf("No curative treatment; remove and destroy infected vines.", "Apply preventive copper sprays."),
                listOf("Use certified healthy planting material.", "Avoid planting in soils with Phaeomoniella.")
            ),
            10 to Triple(
                listOf("Brown spots with yellow margins on leaves.", "Lesions coalesce and leaves fall."),
                listOf("Apply copper-based fungicides.", "Remove infected leaves promptly."),
                listOf("Prune to improve air circulation.", "Avoid overhead irrigation.")
            ),
            11 to Triple(
                listOf("Yellowing of veins and adjacent leaf tissue.", "Fruit is small, lopsided, and poorly colored."),
                listOf("No cure; remove infected trees.", "Control the psyllid vector with insecticides."),
                listOf("Use certified disease-free nursery stock.", "Control psyllid populations.")
            ),
            12 to Triple(
                listOf("Water-soaked spots that turn brown with yellow margins on fruit and leaves.", "Fruit cracks."),
                listOf("Apply copper-based bactericides.", "Remove infected plant material."),
                listOf("Use resistant varieties.", "Avoid overhead irrigation.")
            ),
            13 to Triple(
                listOf("Circular water-soaked lesions on leaves and fruit.", "Lesions become raised, brown, and cracked."),
                listOf("Apply copper bactericides at early signs.", "Remove crop debris after harvest."),
                listOf("Use disease-free transplants.", "Rotate crops for 2-3 years.")
            ),
            14 to Triple(
                listOf("Brown circular lesions with concentric rings.", "Target-board pattern on older leaves."),
                listOf("Apply chlorothalonil or mancozeb.", "Hill soil to protect stems."),
                listOf("Rotate crops with non-solanaceous plants.", "Use certified seed potatoes.")
            ),
            15 to Triple(
                listOf("Water-soaked green-brown lesions, often with white mold at leaf margin.", "Rapid collapse in wet weather."),
                listOf("Apply metalaxyl or dimethomorph fungicides urgently.", "Destroy infected tubers and plant material."),
                listOf("Use certified seed potatoes and resistant varieties.", "Ensure good drainage.")
            ),
            16 to Triple(
                listOf("White powdery fungal patches on leaves.", "Affected tissue turns yellow then brown."),
                listOf("Apply sulfur or potassium bicarbonate sprays.", "Remove badly infected leaves."),
                listOf("Avoid dense planting.", "Ensure good air circulation.")
            ),
            17 to Triple(
                listOf("Irregular reddish-purple spots on leaves.", "Leaves dry out and curl upward."),
                listOf("Apply copper fungicides.", "Remove infected leaves."),
                listOf("Avoid overhead watering.", "Apply mulch to prevent soil splash.")
            ),
            18 to Triple(
                listOf("Small, raised, water-soaked spots on leaves.", "Spots turn brown with yellow halo."),
                listOf("Apply copper-based bactericides.", "Remove infected plant material."),
                listOf("Use disease-free transplants.", "Rotate crops for 2-3 years.")
            ),
            19 to Triple(
                listOf("Dark brown concentric rings on older leaves.", "Lesions with yellow halos."),
                listOf("Apply chlorothalonil or mancozeb.", "Remove and destroy infected leaves."),
                listOf("Rotate tomato crops.", "Stake plants to improve airflow.")
            ),
            20 to Triple(
                listOf("Water-soaked dark patches, white mold on leaf undersides.", "Rapid spread in wet/cool conditions."),
                listOf("Apply metalaxyl fungicides immediately.", "Remove all infected material."),
                listOf("Improve drainage.", "Avoid planting tomatoes near potatoes.")
            ),
            21 to Triple(
                listOf("Pale yellowish-green spots on upper leaf surface.", "Olive-grey mold on underside."),
                listOf("Apply chlorothalonil or copper fungicide.", "Improve greenhouse ventilation."),
                listOf("Plant resistant varieties.", "Maintain low humidity in greenhouses.")
            ),
            22 to Triple(
                listOf("Circular water-soaked spots with dark border.", "Greyish-white centres with black dots."),
                listOf("Apply chlorothalonil or mancozeb.", "Remove lower infected leaves."),
                listOf("Mulch to prevent soil splash.", "Stake plants to improve airflow.")
            ),
            23 to Triple(
                listOf("Fine webbing on leaf undersides.", "Stippled, yellowing leaves that dry up."),
                listOf("Apply miticides (abamectin, bifenazate).", "Use strong water sprays on leaf undersides."),
                listOf("Monitor with sticky traps.", "Avoid plant stress from drought or excess nitrogen.")
            ),
            24 to Triple(
                listOf("Brown circular spots with concentric rings and yellow halo.", "Target-board appearance."),
                listOf("Apply chlorothalonil or mancozeb.", "Remove and destroy infected leaves."),
                listOf("Rotate crops.", "Keep foliage dry with drip irrigation.")
            ),
            25 to Triple(
                listOf("Severe yellowing and upward curling of leaves.", "Stunted plant growth."),
                listOf("No cure; remove and destroy infected plants.", "Control whitefly vector with insecticides."),
                listOf("Use reflective mulches to deter whiteflies.", "Plant resistant varieties.")
            ),
            26 to Triple(
                listOf("Mosaic pattern of light and dark green on leaves.", "Leaf distortion and bronzing."),
                listOf("Remove and destroy infected plants.", "Disinfect tools between plants."),
                listOf("Control aphid vectors.", "Wash hands and tools before handling plants.")
            )
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Interpreter initialization (lazy, with GPU → CPU fallback)
    // ──────────────────────────────────────────────────────────────────────────

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    @Synchronized
    private fun getInterpreter(): Interpreter {
        if (interpreter == null) {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_ASSET)

            // Try GPU delegate first; fall back to CPU if unsupported
            val options = Interpreter.Options().setNumThreads(4)
            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate!!)
                interpreter = Interpreter(modelBuffer, options)
                Log.i(TAG, "LiteRT GPU delegate initialised successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "GPU delegate unavailable, falling back to CPU: ${e.message}")
                gpuDelegate?.close()
                gpuDelegate = null
                interpreter = Interpreter(modelBuffer, Interpreter.Options().setNumThreads(4))
            }
        }
        return interpreter!!
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun detectDisease(imageUri: Uri, cropName: String?): LocalDiseaseResult =
        withContext(Dispatchers.IO) {
            try {
                val rawBitmap = loadBitmap(imageUri)
                val (letterboxed, meta) = letterbox(rawBitmap, INPUT_SIZE)

                val inputBuffer = bitmapToByteBuffer(letterboxed)
                val outputBuffer = Array(1) { Array(NUM_CLASSES + 4) { FloatArray(8400) } }

                getInterpreter().run(inputBuffer, outputBuffer)

                val detection = postProcess(outputBuffer[0], meta)
                    ?: return@withContext noDetectionResult()

                val classIndex = detection.classIndex
                val info = DISEASE_INFO[classIndex]

                LocalDiseaseResult(
                    diseaseName = CLASS_NAMES.getOrElse(classIndex) { "Unknown" },
                    confidence = (detection.confidence * 100).toInt(),
                    symptoms = info?.first ?: listOf("Inspect leaves, stems, and roots for visible damage."),
                    treatment = info?.second ?: listOf("Consult a local agricultural extension officer."),
                    prevention = info?.third ?: listOf("Maintain good crop hygiene and monitor regularly."),
                    additionalNotes = "Detected on-device by YOLO11n · LiteRT FP16 · " +
                        if (gpuDelegate != null) "GPU accelerated" else "CPU fallback"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                LocalDiseaseResult(
                    diseaseName = "Analysis Failed",
                    confidence = 0,
                    symptoms = emptyList(),
                    treatment = emptyList(),
                    prevention = emptyList(),
                    additionalNotes = "Error: ${e.message}"
                )
            }
        }

    // ──────────────────────────────────────────────────────────────────────────
    // Preprocessing
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Letterbox-resize [src] to a [targetSize]×[targetSize] canvas.
     * Preserves aspect ratio by padding unused regions with (114, 114, 114) grey,
     * which matches the padding colour used during YOLO training.
     *
     * Returns the letterboxed bitmap and the metadata needed to map
     * detected boxes back to original image coordinates.
     */
    private fun letterbox(src: Bitmap, targetSize: Int): Pair<Bitmap, LetterboxMeta> {
        val scale = min(
            targetSize.toFloat() / src.width,
            targetSize.toFloat() / src.height
        )
        val scaledW = (src.width * scale).toInt()
        val scaledH = (src.height * scale).toInt()
        val padLeft = (targetSize - scaledW) / 2
        val padTop = (targetSize - scaledH) / 2

        val canvas = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val c = Canvas(canvas)
        c.drawColor(Color.rgb(114, 114, 114)) // YOLO training pad colour

        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        c.drawBitmap(scaled, padLeft.toFloat(), padTop.toFloat(), Paint())
        scaled.recycle()

        return canvas to LetterboxMeta(scale, padLeft, padTop, src.width, src.height)
    }

    /**
     * Convert bitmap to a Float32 ByteBuffer normalized to [0, 1].
     * Format: NHWC (height × width × channels) as required by TFLite.
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buf.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (px in pixels) {
            buf.putFloat(((px shr 16) and 0xFF) / 255f) // R
            buf.putFloat(((px shr 8) and 0xFF) / 255f)  // G
            buf.putFloat((px and 0xFF) / 255f)            // B
        }
        return buf
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Post-processing (Decode + NMS)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Post-process YOLO11 detection output tensor [31, 8400].
     *
     * Ultralytics TFLite export decodes DFL boxes and sigmoid-normalizes class
     * scores internally, so output is already [cx, cy, w, h, c0..c26] in
     * *normalized* coordinates (0-1 relative to input size).
     *
     * Steps:
     *  1. For each of 8400 anchors, find max class score
     *  2. Filter by [CONFIDENCE_THRESHOLD]
     *  3. Decode normalized cx/cy/w/h → x1/y1/x2/y2 in original-image pixels
     *  4. Non-Maximum Suppression
     *  5. Return the highest-confidence surviving detection
     */
    private fun postProcess(
        output: Array<FloatArray>, // shape [31][8400]
        meta: LetterboxMeta
    ): Detection? {
        val candidates = mutableListOf<Detection>()

        for (anchor in 0 until 8400) {
            val cx = output[0][anchor]
            val cy = output[1][anchor]
            val w  = output[2][anchor]
            val h  = output[3][anchor]

            // Find best class score for this anchor
            var bestClass = 0
            var bestScore = 0f
            for (c in 0 until NUM_CLASSES) {
                val score = output[4 + c][anchor]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }

            if (bestScore < CONFIDENCE_THRESHOLD) continue

            // Decode normalized xywh → pixel x1y1x2y2 in original image space
            val x1 = ((cx - w / 2f) * INPUT_SIZE - meta.padLeft) / meta.scale
            val y1 = ((cy - h / 2f) * INPUT_SIZE - meta.padTop) / meta.scale
            val x2 = ((cx + w / 2f) * INPUT_SIZE - meta.padLeft) / meta.scale
            val y2 = ((cy + h / 2f) * INPUT_SIZE - meta.padTop) / meta.scale

            val clampedX1 = x1.coerceIn(0f, meta.origW.toFloat())
            val clampedY1 = y1.coerceIn(0f, meta.origH.toFloat())
            val clampedX2 = x2.coerceIn(0f, meta.origW.toFloat())
            val clampedY2 = y2.coerceIn(0f, meta.origH.toFloat())

            if (clampedX2 <= clampedX1 || clampedY2 <= clampedY1) continue

            candidates.add(
                Detection(bestClass, bestScore, clampedX1, clampedY1, clampedX2, clampedY2)
            )
        }

        if (candidates.isEmpty()) return null
        return nms(candidates).maxByOrNull { it.confidence }
    }

    /**
     * Non-Maximum Suppression — removes overlapping boxes above [IOU_THRESHOLD].
     * Processes all classes together (class-agnostic NMS, suitable for single-detection use).
     */
    private fun nms(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { iou(best, it) >= IOU_THRESHOLD }
        }
        return result
    }

    private fun iou(a: Detection, b: Detection): Float {
        val interX1 = max(a.x1, b.x1)
        val interY1 = max(a.y1, b.y1)
        val interX2 = min(a.x2, b.x2)
        val interY2 = min(a.y2, b.y2)

        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        if (interArea == 0f) return 0f

        val aArea = (a.x2 - a.x1) * (a.y2 - a.y1)
        val bArea = (b.x2 - b.x1) * (b.y2 - b.y1)
        return interArea / (aArea + bArea - interArea)
    }

    private fun noDetectionResult() = LocalDiseaseResult(
        diseaseName = "No Disease Detected",
        confidence = 0,
        symptoms = listOf("No significant disease markers were found in this image."),
        treatment = listOf("Continue regular crop monitoring."),
        prevention = listOf("Maintain good field hygiene and balanced nutrition."),
        additionalNotes = "Try retaking the photo in good lighting, close to the affected leaf."
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Bitmap loading
    // ──────────────────────────────────────────────────────────────────────────

    private fun loadBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(context.contentResolver, uri)
            ) { decoder, _, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

    // ──────────────────────────────────────────────────────────────────────────
    // Data classes
    // ──────────────────────────────────────────────────────────────────────────

    private data class LetterboxMeta(
        val scale: Float,
        val padLeft: Int,
        val padTop: Int,
        val origW: Int,
        val origH: Int
    )

    private data class Detection(
        val classIndex: Int,
        val confidence: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    )
}
