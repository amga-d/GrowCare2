package com.example.growCare.data.local.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * On-device plant disease detector powered by YOLO11n-cls via LiteRT.
 *
 * Model:   disease_detection.tflite (bundled in assets)
 * Format:  TFLite FP16 — exported with `model.export(format='tflite', half=True)`
 * Task:    Classification
 * Classes: 38  (PlantVillage)
 *
 * I/O:
 *   Input  → [1, H, W, 3] float32, normalized 0-1 (H/W read dynamically)
 *   Output → [1, 38] float32 class probabilities
 */
@Singleton
class YoloDiseaseInference @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalDiseaseInference {

    companion object {
        private const val TAG = "YoloDiseaseInference"
        private const val MODEL_ASSET = "disease_detection.tflite"
        private const val NUM_CLASSES = 38
        private const val CONFIDENCE_THRESHOLD = 0.60f // Reject anything below 60 % — very confident required
        private const val HEALTHY_THRESHOLD = 0.50f    // Healthy label needs at least 50 %

        /**
         * Exact class names matching the classification model's output.
         * Must match the classes.txt export order.
         */
        val CLASS_NAMES = listOf(
            "Apple - Apple Scab",
            "Apple - Black Rot",
            "Apple - Cedar Apple Rust",
            "Apple - Healthy",
            "Blueberry - Healthy",
            "Cherry - Powdery Mildew",
            "Cherry - Healthy",
            "Corn - Cercospora Leaf Spot",
            "Corn - Common Rust",
            "Corn - Northern Leaf Blight",
            "Corn - Healthy",
            "Grape - Black Rot",
            "Grape - Esca (Black Measles)",
            "Grape - Leaf Blight",
            "Grape - Healthy",
            "Orange - Citrus Greening",
            "Peach - Bacterial Spot",
            "Peach - Healthy",
            "Pepper, Bell - Bacterial Spot",
            "Pepper, Bell - Healthy",
            "Potato - Early Blight",
            "Potato - Late Blight",
            "Potato - Healthy",
            "Raspberry - Healthy",
            "Soybean - Healthy",
            "Squash - Powdery Mildew",
            "Strawberry - Leaf Scorch",
            "Strawberry - Healthy",
            "Tomato - Bacterial Spot",
            "Tomato - Early Blight",
            "Tomato - Late Blight",
            "Tomato - Leaf Mold",
            "Tomato - Septoria Leaf Spot",
            "Tomato - Spider Mites",
            "Tomato - Target Spot",
            "Tomato - Yellow Leaf Curl Virus",
            "Tomato - Mosaic Virus",
            "Tomato - Healthy"
        )

        private val HEALTHY_INFO = Triple(
            listOf("No visible disease symptoms detected."),
            listOf("Maintain current crop management practices."),
            listOf("Continue regular scouting to catch early signs of stress.")
        )
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var inputHeight = 224
    private var inputWidth = 224

    private suspend fun getInterpreter(): Interpreter = withContext(Dispatchers.IO) {
        if (interpreter == null) {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_ASSET)
            val options = Interpreter.Options()

            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
                interpreter = Interpreter(modelFile, options)
                Log.i(TAG, "LiteRT GPU delegate initialised successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "GPU delegate failed. Falling back to CPU.", e)
                gpuDelegate?.close()
                gpuDelegate = null
                interpreter = Interpreter(modelFile, Interpreter.Options())
            }
            
            // Read input tensor shape dynamically
            val inputShape = interpreter!!.getInputTensor(0).shape()
            Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Input type: ${interpreter!!.getInputTensor(0).dataType()}")
            
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            Log.d(TAG, "Output shape: ${outputShape.contentToString()}")
            Log.d(TAG, "Output type: ${interpreter!!.getOutputTensor(0).dataType()}")
            
            if (inputShape.size == 4) {
                // Usually [1, H, W, 3] or [1, 3, H, W]
                // We'll assume NHWC since that's standard for Android TFLite
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
            }
        }
        return@withContext interpreter!!
    }

    override suspend fun detectDisease(imageUri: Uri, cropName: String?): LocalDiseaseResult = withContext(Dispatchers.Default) {
        try {
            val interp = getInterpreter()
            val bitmap = loadBitmap(imageUri)
            
            // Center crop and resize to exactly match model input dimensions
            val croppedBitmap = centerCropAndResize(bitmap, inputWidth, inputHeight)
            val inputBuffer = convertBitmapToByteBuffer(croppedBitmap, inputWidth, inputHeight)

            // Output tensor: [1, 38]
            val output = Array(1) { FloatArray(NUM_CLASSES) }
            
            val startTime = System.currentTimeMillis()
            interp.run(inputBuffer, output)
            val endTime = System.currentTimeMillis()
            Log.d(TAG, "Inference completed in ${endTime - startTime} ms")

            // Post-process: argmax
            var bestIndex = 0
            var bestScore = output[0][0]

            for (i in 1 until NUM_CLASSES) {
                if (output[0][i] > bestScore) {
                    bestScore = output[0][i]
                    bestIndex = i
                }
            }
            
            // Debug: print top 3 scores to see what the model is doing
            val top3 = output[0].withIndex().sortedByDescending { it.value }.take(3)
            Log.d(TAG, "Top 3 predictions: " + top3.joinToString { "${CLASS_NAMES.getOrNull(it.index)}: ${it.value}" })

            if (bestScore < CONFIDENCE_THRESHOLD) {
                return@withContext noDetectionResult()
            }

            val className = CLASS_NAMES.getOrElse(bestIndex) { "Unknown" }

            LocalDiseaseResult(
                diseaseName = className,
                confidence = (bestScore * 100).toInt(),
                symptoms = emptyList(),
                treatment = emptyList(),
                prevention = emptyList(),
                additionalNotes = "Classified on-device by YOLO11n-cls · LiteRT FP16 · " +
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

    /**
     * Center crops the image to a square, then resizes it to target size.
     * This avoids aspect ratio distortion which hurts classification performance.
     */
    private fun centerCropAndResize(src: Bitmap, width: Int, height: Int): Bitmap {
        val minDim = min(src.width, src.height)
        val x = (src.width - minDim) / 2
        val y = (src.height - minDim) / 2
        
        val squared = Bitmap.createBitmap(src, x, y, minDim, minDim)
        if (squared != src) {
            src.recycle()
        }
        
        val resized = Bitmap.createScaledBitmap(squared, width, height, true)
        if (resized != squared) {
            squared.recycle()
        }
        
        return resized
    }

    /**
     * Convert bitmap to a Float32 ByteBuffer normalized to [0, 1].
     * Format: NHWC (height × width × channels) as required by TFLite.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap, width: Int, height: Int): ByteBuffer {
        // 4 bytes per float * 3 channels (RGB)
        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            // Extract RGB and normalize to 0.0 - 1.0 (matching YOLO training)
            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    private fun noDetectionResult() = LocalDiseaseResult(
        diseaseName = "Unable to Classify",
        confidence = 0,
        symptoms = listOf("The model could not classify the image with sufficient confidence."),
        treatment = listOf("Ensure the image is well-lit and clearly shows the affected plant leaf."),
        prevention = listOf("Try retaking the photo closer to the subject."),
        additionalNotes = "Prediction confidence was below the minimum threshold ($CONFIDENCE_THRESHOLD)."
    )

    private fun loadBitmap(uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
        return rotateBitmapIfRequired(bitmap, uri)
    }

    /**
     * Reads the EXIF orientation tag from the image file and rotates the Bitmap
     * so that camera photos are correctly oriented before being fed to YOLO.
     * Models struggle heavily if fed a 90-degree sideways image.
     */
    private fun rotateBitmapIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    else -> return bitmap
                }

                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                return rotated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF orientation, falling back to original bitmap", e)
        }
        return bitmap
    }
}
