# GrowCare2 Offline-First Version Plan

**Date**: 2026-07-01  
**Goal**: Rebrand the product as GrowCare2 and replace cloud dependencies with a fully offline-first architecture centered on on-device disease detection and an on-device agricultural assistant.

## 1. Current State Summary

The current app is already organized around Clean Architecture and MVVM, with:

- Jetpack Compose UI screens
- ViewModels using StateFlow and SharedFlow
- UseCases and repository interfaces in the domain layer
- Data sources for Firebase, local Room history, and Gemini-based remote AI

The new version removes all cloud API dependence from the core product surface, including:

- Weather API usage
- Authentication services
- Storage uploads for AI features
- Database-backed cloud sync for AI workflows

The implementation also still exposes features that are no longer part of the new scope:

- Seed quality assessment
- Fertilizer recipe generation
- Cloud-first Gemini analysis paths
- Firebase-dependent chat and disease analysis flows

The current navigation and feature layout show that disease, chat, history, and profile are already separated enough to refactor instead of rewriting the whole app.

## 2. Target Product Scope

The new version of GrowCare2 should focus on only two AI experiences:

1. Disease detection from leaf images
2. AI agricultural assistant chat

Removed from product scope:

- Seed quality scanning
- Fertilizer recipe generation
- Any feature that requires cloud LLM calls for core behavior

The app should work offline for inference after the models are shipped or first loaded locally, with no requirement for weather, authentication, storage, or database cloud services.

## 3. Target Offline-First Architecture

### Disease Detection Flow

```text
Smartphone Camera
  -> Image Acquisition
  -> Image Pre-processing
  -> YOLO11s Object Detection
  -> Disease name + confidence
  -> Prompt construction
  -> Gemma 3n on-device LLM
  -> Natural language explanation
  -> Result screen
```

### Agricultural Assistant Flow

```text
Farmer question
  -> Chat interface
  -> Prompt construction
  -> Gemma 3n on-device LLM
  -> Response generation
  -> Chat interface
```

## 4. Recommended Layer Map

### Presentation Layer

Keep:

- Compose screens
- Navigation
- Theme system
- ViewModel-based state handling

Replace:

- Seed and fertilizer screens
- Gemini-specific loading states and prompts
- Cloud upload-driven result handling for the core flows

### Domain Layer

Keep:

- UseCase pattern
- Repository abstractions
- Domain models for disease analysis, chat messages, and history

Replace:

- SeedQuality and FertilizerRecommendation domain models
- UseCases that only serve removed features

### Data Layer

Keep:

- Room database for local history and caching
- Local repository implementations
- Existing local datasource patterns

Replace:

- Gemini remote client as the primary AI path
- Firebase and any cloud API as the default backend for core features
- Cloud upload steps that exist only to support AI inference

## 5. Proposed Runtime Architecture

### A. Disease Pipeline

1. Camera capture returns a local image URI.
2. Image preprocessing resizes, normalizes, and prepares the input tensor.
3. YOLO11s runs on-device and returns top disease candidates.
4. The app constructs a compact prompt using the detected disease name, confidence, and user language.
5. Gemma 3n runs locally and generates the farmer-facing explanation.
6. The app stores the scan locally and shows the result immediately.

### B. Chat Pipeline

1. User asks a question in chat.
2. The app builds a system prompt with agricultural guardrails.
3. Gemma 3n responds locally, optionally streaming token chunks if the runtime supports it.
4. The app stores the conversation locally for history and reuse.

## 6. What Should Be Removed or Retired

High-priority removals:

- Seed scan screens, models, repositories, use cases, and history routes
- Fertilizer screens, models, repositories, use cases, and history routes
- Cloud Gemini-specific network path as the primary inference engine
- Firebase Storage usage for AI feature uploads if no longer needed
- Weather API, authentication, and cloud database dependencies from the core app flow

Likely cleanups:

- Navigation routes for removed features
- History filters for seed and fertilizer
- UI actions and ViewModel branches for removed features
- Build config and dependencies that only support cloud AI calls

## 7. What Should Be Kept

Keep these because they support the new scope or can be reused with minimal change:

- App shell, theme, Compose structure, and navigation foundation
- Local Room history for scans and chat
- History screen layout if it can be narrowed to disease + chat only
- Profile screen if it remains relevant for language/model settings

## 8. Implementation Phases

### Phase 1: Scope Reduction and Architecture Cleanup

- Remove seed and fertilizer feature routes, screens, ViewModels, use cases, and repository bindings
- Update domain models to match the new product scope
- Simplify navigation to disease, chat, history, profile, and auth only if needed
- Remove cloud AI assumptions and cloud service assumptions from docs and build files

### Phase 2: On-Device Model Runtime

- Choose the mobile runtime for YOLO11s and Gemma 3n
- Define the model asset loading strategy for first run and subsequent runs
- Add a model manager for versioning, download, verification, and cache invalidation
- Add device capability checks and graceful fallback messaging

### Phase 3: Disease Detection Pipeline

- Implement image preprocessing utilities
- Integrate YOLO11s inference
- Map model outputs to domain-level disease candidates
- Build prompt construction for Gemma 3n explanation generation
- Add a result presentation flow with offline history saving

### Phase 4: Agricultural Assistant Pipeline

- Replace the current chat backend with local inference
- Create a system prompt tailored to farming guidance
- Add chat history persistence and conversation threading locally
- Support language-aware prompting if multilingual output is needed

### Phase 5: Local Storage and History

- Keep disease scan history in Room
- Keep chat history in Room
- Add repository methods for offline retrieval and deletion
- Remove history views for deleted feature families

### Phase 6: UI and UX Polish

- Update home screen shortcuts to only show supported features
- Add offline status, model loading, and low-memory warnings
- Add a first-run model setup experience
- Update empty states and error messaging for offline inference failures

### Phase 7: Testing and Validation

- Unit test prompt builders, preprocessing, and mapping logic
- Unit test ViewModels and repositories
- Add integration tests for the image-to-result disease flow
- Add chat prompt and response tests with local model abstractions

## 9. Suggested New Module Boundaries

### Presentation

- `presentation/screens/detection/`
- `presentation/screens/chat/`
- `presentation/screens/history/`
- `presentation/screens/profile/`

### Domain

- `domain/model/` for `DiseaseAnalysis`, `ChatMessage`, `Conversation`, and support models
- `domain/usecase/detection/`
- `domain/usecase/chat/`
- `domain/usecase/model/` for model setup and health checks

### Data

- `data/local/database/` for scan and chat history
- `data/local/model/` for local model management
- `data/local/inference/` for YOLO and Gemma wrappers
- `data/repository/` for offline-first repositories

## 10. Migration Risks

- On-device inference can be memory-heavy on low-end devices
- YOLO and Gemma model sizes may require download and storage controls
- Streaming chat may need a fallback if the local runtime only supports batch generation
- Prompt quality and output consistency will depend heavily on local model tuning
- Removing seed/fertilizer features will change navigation and history assumptions across the app

## 11. Immediate Next Decisions

1. Confirm the inference runtime for Android, including model format and execution backend.
2. Decide whether local model packages ship with the app or are downloaded on first run.
3. Confirm whether any profile or identity concept remains local-only or is removed entirely.
4. Confirm whether history should sync later or remain device-local only.

## 12. Recommended First Build Order

1. Remove seed and fertilizer scope.
2. Replace AI repository interfaces with local inference abstractions.
3. Implement a model manager for first-run loading.
4. Build the disease pipeline end to end.
5. Build the chat pipeline end to end.
6. Tighten history, settings, and offline UX.

## 13. Short Version

GrowCare2 is already structured well enough to pivot. The main work is not a redesign of UI patterns, but a replacement of the AI/data backend, the removal of cloud services from the core product flow, and a reduction of product scope to disease detection and agricultural chat.