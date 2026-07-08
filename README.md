# 🌾 GrowCare 2 - Fully Offline AI Agriculture Assistant

> Making invisible agricultural threats visible and preventable, completely offline.

## 📋 Overview

**GrowCare 2** is a revolutionary smart assistant for farmers that transforms smartphone cameras into powerful agricultural diagnostic tools. Rebuilt from the ground up to operate completely offline, GrowCare 2 leverages on-device AI models (YOLO and Gemma via LiteRT) to detect, predict, and prevent crop problems without requiring an internet connection.

Designed for farmers in rural areas with poor connectivity, GrowCare 2 guarantees zero latency, enhanced privacy, and constant accessibility.

## ✨ Key Features

### 1. 🦠 Real-time Disease Detection (YOLO11n-cls)
Snap a photo to instantly identify plant diseases across 38 crop categories. The on-device vision model processes images in milliseconds directly on your phone's processor.

### 2. 💬 Multimodal AI Chat Assistant (Gemma 4 E2B)
A built-in conversational agent powered by Google's Gemma 4 E2B via LiteRT-LM. Ask farming questions or upload photos of your crops—our hybrid AI pipeline allows the text-only Gemma model to "see" images via YOLO and provide tailored, step-by-step agronomic advice.

### 3. 💾 Fully Offline Architecture
Zero reliance on cloud APIs or Firebase. Chat history and disease scans are stored locally using Room (SQLite), ensuring you never lose access to your data or the AI assistant when off the grid.

## 🏗️ Technology & AI Stack

GrowCare 2 utilizes a decoupled, on-device hybrid multimodal pipeline. 

### Frontend & Local Data
- **Kotlin & Jetpack Compose** - Modern, reactive UI.
- **Room (SQLite)** - Local persistence for chat histories and diagnostic logs.
- **Native Camera APIs & EXIF Processing** - Correctly processes camera orientations for reliable vision inference.

### The AI Engine (LiteRT / TFLite)
For deep architectural details, see the [AI System Architecture Document](AI_SYSTEM_ARCHITECTURE.md).

- **Vision: YOLO11n-cls (Ultralytics)**
  - A lightweight convolutional neural network optimized for image classification.
  - Replaces traditional object detection to remove heavy post-processing (NMS), offering 3x faster inference and higher accuracy on the PlantVillage dataset.
- **Language: Gemma 4 E2B (Google LiteRT-LM)**
  - A highly compressed, 2-billion parameter Large Language Model running locally.
  - Dynamically parses YOLO results to generate Markdown-formatted treatments, symptoms, and preventions.
  - Remembers up to 10 conversational turns (20 messages) for deep, context-aware agricultural support.

## 📱 How It Works

```
1. Snap Photo → 2. YOLO Identifies Disease → 3. Gemma Generates Advice → 4. Save Locally
```

### Multimodal Chat Workflow:
1. **Capture** - Take a photo of a sick leaf in the Chat screen.
2. **Vision Analysis** - The YOLO model instantly classifies the disease.
3. **Context Injection** - The classification result is seamlessly injected into the Gemma LLM's prompt.
4. **AI Response** - Gemma streams a comprehensive, context-aware answer as if it saw the photo itself.

## 🚀 Why GrowCare 2?

### vs. Traditional Cloud Solutions
| Solution Type | Limitations | GrowCare 2 Advantage |
| ------------- | ----------- | -------------------- |
| ☁️ Cloud AI Apps | Require strong 4G/5G, high API costs | **100% Offline**, zero latency, free to run |
| 📚 Digital Manuals | Static PDFs, hard to search | **Dynamic AI** that answers specific questions |
| 🧑‍🌾 Expert Consultants | Rare and expensive | **24/7 AI expert** in your pocket |

## 🛠️ Getting Started (Setup & Run)

Follow these instructions to run the application locally on an Android device.

### Prerequisites
1. **Android Studio** (Koala or later recommended)
2. **Physical Android Device** (Emulators are not recommended due to heavy LLM requirements)
3. **Gemma 4 E2B Model** (Download the `.litertlm` file from HuggingFace `litert-community/gemma-4-E2B-it-litert-lm`) or [github](https://github.com/LTERTPub/LiteRT-LM/blob/main/examples/gemma4_e2b_it.litertlm)

### 1. Build the App
1. Clone the repository: `git clone <repo-url>`
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. Install the app on your physical device via USB debugging.

### 2. Push the LLM Model via ADB
Because the Gemma LLM is too large (1-2GB) to bundle inside an APK, you must manually push it to the app's internal storage before chatting with the AI.

1. Connect your Android device to your PC with USB Debugging enabled.
2. Open a terminal and run the following ADB command to push the model:
   ```bash
   adb push <path_to_downloaded_model>/gemma4_e2b.litertlm /data/local/tmp/
   adb shell run-as com.example.growCare mkdir -p files/models
   adb shell run-as com.example.growCare cp /data/local/tmp/gemma4_e2b.litertlm files/models/
   adb shell rm /data/local/tmp/gemma4_e2b.litertlm
   ```
3. Launch the app. The AI Chat and Disease Detection features will now work completely offline!


### 3. Convert and Add YOLO Model (Optional)
If you want to train and add your own YOLO classification model instead of using the default one:

1. Train a YOLO11-cls model using the `ultralytics` package.
2. Place your trained `best.pt` file in the `ML` directory.
3. Convert the trained model to TFLite FP16 format by running the provided export script:
   ```bash
   python ML/export_tflite_fp16.py
7. Rebuild and install the app to test your new model!

## 👥 Team

**Team Leader:** Amgad Al-Ameri (@amga9d)

## 📄 License
This project is part of an AI innovation challenge. Please contact the team for licensing information.

## 🤝 Contributing
We welcome contributions! Please feel free to submit issues and pull requests.

## 🙏 Acknowledgments
- Powered by **Google LiteRT** and **Gemma**.
- Built for farmers worldwide who feed our planet.
---
**Made with ❤️**
_Transforming smartphones into super eyes for sustainable agriculture._
