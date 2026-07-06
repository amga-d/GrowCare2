package com.example.growCare.data.local.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
        private const val CONFIDENCE_THRESHOLD = 0.25f // Reject predictions below this

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

        /** Static agronomic knowledge keyed by class index. */
        private val DISEASE_INFO = mapOf(
            0 to Triple( // Apple___Apple_scab
                listOf("Olive-green to brown velvety lesions on leaves and fruit.", "Distorted shoots and leaves."),
                listOf("Apply fungicides (captan, myclobutanil).", "Remove and destroy infected plant material."),
                listOf("Plant resistant apple varieties.", "Ensure good air circulation.")
            ),
            1 to Triple( // Apple___Black_rot
                listOf("Small, purple-bordered leaf spots with tan centres.", "Shrivelled, mummified fruit."),
                listOf("Apply copper-based fungicides.", "Prune and remove infected wood."),
                listOf("Avoid wounding trees.", "Maintain tree vigor through balanced fertilization.")
            ),
            2 to Triple( // Apple___Cedar_apple_rust
                listOf("Orange rust-colored spots on upper leaf surface.", "Tube-like spore structures underneath."),
                listOf("Apply myclobutanil or triadimefon fungicides.", "Remove heavily infected branches."),
                listOf("Do not plant apples near eastern red cedars.", "Use resistant varieties where available.")
            ),
            3 to HEALTHY_INFO, // Apple___healthy
            4 to HEALTHY_INFO, // Blueberry___healthy
            5 to Triple( // Cherry_(including_sour)___Powdery_mildew
                listOf("White powdery fungal growth on young leaves.", "Leaf distortion and premature drop."),
                listOf("Apply sulfur or potassium bicarbonate sprays.", "Remove infected shoots."),
                listOf("Improve canopy airflow with pruning.", "Avoid excess nitrogen fertilization.")
            ),
            6 to HEALTHY_INFO, // Cherry_(including_sour)___healthy
            7 to Triple( // Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot
                listOf("Small grey or tan rectangular leaf spots.", "Lesions bordered by leaf veins."),
                listOf("Apply strobilurin or triazole fungicides.", "Rotate with non-host crops."),
                listOf("Plant resistant hybrids.", "Avoid planting in fields with previous history.")
            ),
            8 to Triple( // Corn_(maize)___Common_rust_
                listOf("Pustules of orange-red spores on undersides of leaves.", "Yellow-orange discoloration."),
                listOf("Apply triazole or strobilurin fungicides.", "Scout fields regularly after tasseling."),
                listOf("Plant resistant hybrids.", "Monitor weather forecasts for high-risk conditions.")
            ),
            9 to Triple( // Corn_(maize)___Northern_Leaf_Blight
                listOf("Large, elliptical lesions with wavy margins.", "Greyish-green to tan lesions on leaves."),
                listOf("Apply azoxystrobin or propiconazole.", "Plant early to avoid peak infection periods."),
                listOf("Select resistant hybrids.", "Avoid susceptible hybrids in high-risk areas.")
            ),
            10 to HEALTHY_INFO, // Corn_(maize)___healthy
            11 to Triple( // Grape___Black_rot
                listOf("Small, angular reddish-brown lesions on leaves.", "Black shrivelled berries."),
                listOf("Apply copper or myclobutanil fungicides from bud break.", "Remove mummified berries."),
                listOf("Prune for good canopy airflow.", "Remove all infected material after harvest.")
            ),
            12 to Triple( // Grape___Esca_(Black_Measles)
                listOf("Reddish-brown streaks on canes.", "Interveinal yellowing; leaves wilt and dry."),
                listOf("No curative treatment; remove and destroy infected vines.", "Apply preventive copper sprays."),
                listOf("Use certified healthy planting material.", "Avoid planting in soils with Phaeomoniella.")
            ),
            13 to Triple( // Grape___Leaf_blight_(Isariopsis_Leaf_Spot)
                listOf("Brown spots with yellow margins on leaves.", "Lesions coalesce and leaves fall."),
                listOf("Apply copper-based fungicides.", "Remove infected leaves promptly."),
                listOf("Prune to improve air circulation.", "Avoid overhead irrigation.")
            ),
            14 to HEALTHY_INFO, // Grape___healthy
            15 to Triple( // Orange___Haunglongbing_(Citrus_greening)
                listOf("Yellowing of veins and adjacent leaf tissue.", "Fruit is small, lopsided, and poorly colored."),
                listOf("No cure; remove infected trees.", "Control the psyllid vector with insecticides."),
                listOf("Use certified disease-free nursery stock.", "Control psyllid populations.")
            ),
            16 to Triple( // Peach___Bacterial_spot
                listOf("Water-soaked spots that turn brown with yellow margins on fruit and leaves.", "Fruit cracks."),
                listOf("Apply oxytetracycline or copper-based bactericides.", "Remove infected twigs."),
                listOf("Plant resistant peach varieties.", "Avoid excessive nitrogen fertilization.")
            ),
            17 to HEALTHY_INFO, // Peach___healthy
            18 to Triple( // Pepper,_bell___Bacterial_spot
                listOf("Small, yellowish-green to brown spots on leaves.", "Lesions have a slightly raised, scabby appearance."),
                listOf("Apply copper-based fungicides with mancozeb.", "Remove infected plants immediately."),
                listOf("Use certified disease-free seeds.", "Rotate crops; do not plant nightshades in the same soil.")
            ),
            19 to HEALTHY_INFO, // Pepper,_bell___healthy
            20 to Triple( // Potato___Early_blight
                listOf("Dark brown to black lesions with concentric rings on leaves.", "Lower leaves affected first."),
                listOf("Apply chlorothalonil or mancozeb fungicides.", "Ensure plants have adequate nitrogen."),
                listOf("Rotate crops out of nightshade family.", "Remove potato vines after harvest.")
            ),
            21 to Triple( // Potato___Late_blight
                listOf("Water-soaked, dark green to black lesions on leaves.", "White fungal growth on leaf undersides in high humidity."),
                listOf("Apply mefenoxam or chlorothalonil immediately.", "Destroy all infected plants and tubers."),
                listOf("Use certified seed potatoes.", "Avoid overhead irrigation and ensure good drainage.")
            ),
            22 to HEALTHY_INFO, // Potato___healthy
            23 to HEALTHY_INFO, // Raspberry___healthy
            24 to HEALTHY_INFO, // Soybean___healthy
            25 to Triple( // Squash___Powdery_mildew
                listOf("White, powdery fungal spots on leaves and stems.", "Leaves may turn yellow and die prematurely."),
                listOf("Apply sulfur or potassium bicarbonate sprays.", "Remove infected plant debris."),
                listOf("Plant resistant varieties.", "Provide adequate spacing for air circulation.")
            ),
            26 to Triple( // Strawberry___Leaf_scorch
                listOf("Irregular purplish-brown spots on leaves.", "Lesions lack white centers (unlike leaf spot)."),
                listOf("Apply captan or myclobutanil fungicides.", "Remove infected leaves."),
                listOf("Maintain good weed control.", "Avoid dense plantings to improve air flow.")
            ),
            27 to HEALTHY_INFO, // Strawberry___healthy
            28 to Triple( // Tomato___Bacterial_spot
                listOf("Small, dark, water-soaked spots on leaves.", "Spots become angular and turn black."),
                listOf("Apply copper fungicides mixed with mancozeb.", "Avoid working in wet fields."),
                listOf("Use disease-free seed.", "Practice strict crop rotation.")
            ),
            29 to Triple( // Tomato___Early_blight
                listOf("Dark lesions with concentric rings on lower leaves.", "Yellowing around the lesions."),
                listOf("Apply chlorothalonil or copper-based fungicides.", "Remove affected lower leaves."),
                listOf("Stake or cage plants to keep foliage off the ground.", "Mulch to prevent soil splash.")
            ),
            30 to Triple( // Tomato___Late_blight
                listOf("Large, irregular, water-soaked lesions on leaves and stems.", "Rapid defoliation and fruit rot."),
                listOf("Apply chlorothalonil or copper fungicides immediately.", "Remove and destroy severely infected plants."),
                listOf("Ensure good air circulation.", "Avoid overhead watering.")
            ),
            31 to Triple( // Tomato___Leaf_Mold
                listOf("Pale green or yellow spots on upper leaf surface.", "Olive-green to brown velvety mold on underside."),
                listOf("Apply chlorothalonil or mancozeb.", "Improve greenhouse ventilation."),
                listOf("Use resistant varieties.", "Maintain humidity below 85%.")
            ),
            32 to Triple( // Tomato___Septoria_leaf_spot
                listOf("Small, circular spots with dark borders and grey centers.", "Tiny black specks (pycnidia) in the center of spots."),
                listOf("Apply chlorothalonil or copper fungicides.", "Remove infected lower leaves."),
                listOf("Rotate crops.", "Water at the base of the plant to keep leaves dry.")
            ),
            33 to Triple( // Tomato___Spider_mites Two-spotted_spider_mite
                listOf("Tiny yellow or white speckles on leaves.", "Fine webbing visible on undersides of leaves."),
                listOf("Apply horticultural oils or insecticidal soaps.", "Release predatory mites (e.g., Phytoseiulus persimilis)."),
                listOf("Maintain adequate irrigation; mites thrive on stressed plants.", "Control broadleaf weeds.")
            ),
            34 to Triple( // Tomato___Target_Spot
                listOf("Dark brown lesions with concentric rings, often with a yellow halo.", "Lesions may coalesce and cause leaf blight."),
                listOf("Apply chlorothalonil or azoxystrobin fungicides.", "Remove infected leaves and debris."),
                listOf("Improve air circulation.", "Avoid overhead irrigation.")
            ),
            35 to Triple( // Tomato___Tomato_Yellow_Leaf_Curl_Virus
                listOf("Upward curling and yellowing of leaf margins.", "Stunted plant growth and reduced fruit set."),
                listOf("No cure; remove and destroy infected plants immediately.", "Control whitefly populations with insecticides or oils."),
                listOf("Plant resistant varieties.", "Use reflective mulches to repel whiteflies.")
            ),
            36 to Triple( // Tomato___Tomato_mosaic_virus
                listOf("Mottled light and dark green patterns on leaves.", "Stunted growth and distorted fruit."),
                listOf("No cure; remove and destroy infected plants.", "Wash hands and tools thoroughly with soap after handling."),
                listOf("Use certified disease-free seeds.", "Avoid using tobacco products near plants (can transmit TMV).")
            ),
            37 to HEALTHY_INFO // Tomato___healthy
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

            if (bestScore < CONFIDENCE_THRESHOLD) {
                return@withContext noDetectionResult()
            }

            val info = DISEASE_INFO[bestIndex]
            val className = CLASS_NAMES.getOrElse(bestIndex) { "Unknown" }

            LocalDiseaseResult(
                diseaseName = className,
                confidence = (bestScore * 100).toInt(),
                symptoms = info?.first ?: listOf("Inspect leaves, stems, and roots for visible damage."),
                treatment = info?.second ?: listOf("Consult a local agricultural extension officer."),
                prevention = info?.third ?: listOf("Maintain good crop hygiene and monitor regularly."),
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
}
