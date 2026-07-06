# GrowCare YOLO Model Flow & Architecture

This document explains exactly how the YOLO model is integrated into the GrowCare Android app, tracking an image from the camera all the way to the final UI result.

---

> [!WARNING]
> **CRITICAL MISMATCH DETECTED**
> I noticed you just ran `export_tflite_fp16.py` and exported a **YOLO11n-cls (Classification)** model with an output shape of `[1, 38]`.
> 
> The GrowCare Android app is currently programmed to read a **YOLO11n (Object Detection)** model with an output shape of `[1, 31, 8400]`. If you run the app with this new classification model, the app will crash because the tensor shapes do not match. 
> 
> **To fix this:** You must either re-export a detection model (`yolo11n.pt`) OR we need to rewrite the Android Kotlin code to handle a classification model instead of a detection model.

---

## 1. Input Preprocessing (Image to Tensor)
Before the AI can see the image, it must be formatted into the exact mathematical shape the TFLite model expects: `[1, 640, 640, 3]`.

**Steps in Kotlin (`YOLO11n`):**
1. **Letterbox Resizing:** The user takes a photo (e.g., `1080x1920`). We cannot stretch the image, or the leaf will look distorted to the AI. Instead, we shrink the image so the longest side is `640` pixels, and pad the remaining space with a neutral grey color `(114, 114, 114)`. This preserves the aspect ratio.
2. **Normalization:** The image pixels are converted from `0-255` integers into `0.0 - 1.0` floating-point numbers.
3. **Tensor Creation:** The pixels are flattened into a `ByteBuffer` representing the shape `[Batch: 1, Width: 640, Height: 640, Channels: 3 (RGB)]`.

## 2. On-Device Inference (LiteRT)
The normalized `ByteBuffer` is passed to Google's **LiteRT** (formerly TFLite) engine.

- **Hardware Acceleration:** The app attempts to use the **GPU Delegate**. If the phone's GPU supports the mathematical operations, it will process the image in milliseconds. If the GPU fails or is unsupported, it gracefully falls back to the **XNNPACK CPU Delegate**.
- **Execution:** The AI processes the image through its neural network layers.

## 3. Output Post-Processing (Tensor to UI)
*(Note: This describes the Object Detection code currently in the app)*

The TFLite model outputs a massive tensor of shape `[1, 31, 8400]`. 
- **8400** represents the number of "anchors" (grid cells looking for an object).
- **31** represents the data for each anchor:
  - `Indices 0-3`: Bounding box coordinates (`cx, cy, w, h`)
  - `Indices 4-30`: Confidence scores for the 27 plant diseases.

**Steps in Kotlin (`postProcess`):**
1. **Loop through 8,400 anchors:** For every single anchor, the code looks at the 27 disease scores and finds the highest one.
2. **Confidence Threshold:** If the highest score is below `25%` (0.25), the anchor is discarded as background noise.
3. **Decode Coordinates:** If the score is high enough, the `cx, cy, w, h` values (which are normalized between `0-1`) are converted back into actual pixel coordinates on the original image.
4. **Non-Maximum Suppression (NMS):** Because YOLO looks at 8,400 grid cells, a single diseased leaf might be detected by 10 overlapping boxes. NMS compares the boxes, keeps the one with the highest confidence, and deletes the duplicates.
5. **Sanity Check Guard:** If the model outputs *too many* confident boxes (e.g., >1,200), the app recognizes the model is untrained/outputting garbage and safely returns "Unable to Analyze".

## 4. Final Result Mapping
Once the single best detection is found (e.g., `Class 14, Confidence 89%`), the app maps `Class 14` to a hardcoded database (`DISEASE_INFO`). 

This database provides the human-readable strings:
- **Name:** "Potato Early Blight"
- **Symptoms:** "Dark lesions with concentric rings..."
- **Treatment:** "Apply copper-based fungicides..."

This data is packaged into a `LocalDiseaseResult` object and sent to the UI to be displayed to the user.
