# GrowCare AI System Architecture

## 1. Overview
GrowCare employs a fully offline, on-device AI architecture to ensure privacy, zero latency, and accessibility for farmers in rural areas with poor internet connectivity. The system relies on two primary on-device models working in tandem:
1. **YOLO11n-cls (Ultralytics)**: A lightweight convolutional neural network for rapid plant disease classification.
2. **Gemma 4 E2B (Google LiteRT-LM)**: A quantized 2-billion parameter Large Language Model for generating dynamic agronomic advice and powering the conversational agent.

This architecture decouples vision from language generation, enabling a hybrid multimodal approach where the vision model acts as the "eyes" and the LLM acts as the "brain."

---

## 2. Architectural Decisions

### 2.1 Fully On-Device Execution
**Decision:** All inferences are run locally on the mobile device rather than relying on a cloud backend (e.g., Firebase, AWS).
**Rationale:** Target users (farmers) often operate in areas with intermittent or zero internet connectivity. On-device processing guarantees that the app remains functional in the field, eliminates API latency, and preserves data privacy.

### 2.2 Classification over Object Detection
**Decision:** Transitioned the YOLO model from an Object Detection architecture (`YOLO11n`) to an Image Classification architecture (`YOLO11n-cls`).
**Rationale:** Object detection requires computationally expensive post-processing (e.g., Non-Maximum Suppression (NMS), bounding box decoding). Since the primary goal is identifying the disease in a focused image of a leaf, classification provides higher accuracy on the 38-class PlantVillage dataset, consumes significantly less memory, and runs up to 3x faster on mobile CPUs.

### 2.3 Decoupled Multimodal Pipeline
**Decision:** Instead of using a heavy, natively multimodal model (which exceeds mobile hardware constraints), the system simulates multimodal behavior by chaining YOLO and Gemma sequentially.
**Rationale:** Gemma 4 E2B via LiteRT-LM on Android is currently text-only. By passing the image through YOLO first and injecting its high-confidence text prediction into Gemma's prompt, the system achieves "vision-language" capabilities while remaining lightweight enough to run on mid-range Android devices.

---

## 3. Disease Detection Pipeline

The disease detection pipeline transforms raw pixels from the device camera into actionable, generated agricultural advice.

### Step-by-Step Flow:
1. **Input Preprocessing:** 
   - The user captures an image.
   - The system reads the EXIF metadata and applies a rotation matrix to ensure portrait photos are correctly oriented upright (preventing rotational accuracy drops).
   - The image is center-cropped to a 1:1 aspect ratio to avoid distortion and resized to `224x224` pixels.
   - The RGB pixel values are normalized to a `[0, 1]` Float32 `ByteBuffer`.

2. **YOLO Inference:**
   - The buffer is passed to the TFLite (`disease_detection.tflite`) model running via the LiteRT GPU/CPU delegate.
   - The model outputs a `[1, 38]` tensor representing the probability distribution across 38 PlantVillage classes.
   - A strict confidence threshold (`0.60`) is applied to filter out non-plant or heavily obscured images.

3. **Dynamic Advice Generation (Gemma):**
   - If a disease is detected with high confidence (e.g., "Tomato - Early Blight"), the class name is passed to the LLM layer (`GemmaChatInference`).
   - A strict system prompt forces Gemma to generate exactly 2 Symptoms, 2 Treatments, and 2 Preventions in a specific Markdown format.
   - The output is parsed via Regex by the Repository layer into structured lists and displayed on the UI.

---

## 4. Chat AI Assistant Pipeline

The Chat AI pipeline provides a persistent, memory-aware conversational interface.

### Step-by-Step Flow:
1. **Context Retrieval:**
   - When a user sends a message, `ChatRepositoryImpl` retrieves the `conversationId` history from the local SQLite (Room) database.
   - To balance memory performance and context awareness, the system truncates the context window to the last 10 messages (5 full back-and-forth turns).

2. **Prompt Construction:**
   - The system builds a structured prompt containing the System Guidelines, the previous conversation history, and the new user message.

3. **Multimodal Injection (Optional):**
   - If the user attaches an image to the message, the image is first routed through the YOLO pipeline (Step 3).
   - The text prompt is enriched dynamically: *"I have uploaded an image. Our vision system detected: [Disease] with [Confidence]%. Based on this, my question is: [User Message]"*.
   - This effectively gives the text-only Gemma model the context of the user's photo.

4. **Streaming Inference:**
   - The prompt is dispatched to the `gemma4_e2b.litertlm` engine via `LiteRT-LM`.
   - The generation uses strict constraints (`MAX_TOKENS = 768`, Repetition Guardrails) to prevent the small 2B model from looping or consuming excessive battery.
   - Tokens are streamed asynchronously via Kotlin `Flow` directly to the Compose UI for a seamless, low-latency typing effect.

5. **Persistence:**
   - Once generation is complete (or aborted via guards), the final complete string is saved back to the Room database, updating the context for the next turn.
