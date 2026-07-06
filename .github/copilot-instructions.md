# GrowCare2 - AI Coding Assistant Instructions

**Project**: GrowCare2 - Offline-First Agricultural Management Android Application  
**Package**: `com.example.growCare`  
**Architecture**: MVVM (Model-View-ViewModel)  
**UI Framework**: Jetpack Compose with Material3  
**Target SDK**: 36 | Min SDK: 24  
**Last Updated**: December 9, 2025

---

## Migration Override (July 2026)

The sections below include legacy implementation details from the previous cloud-connected app. For all new work, follow these rules first:

- Treat GrowCare2 as an offline-first product with local inference.
- Do not implement or re-introduce weather API, cloud authentication, cloud storage, or cloud database requirements in core flows.
- Keep scope limited to disease detection and AI agricultural assistant features.
- Seed quality and fertilizer features are deprecated and should be removed from active flows.

When a legacy section conflicts with this override, the override takes priority.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture Guidelines](#architecture-guidelines)
4. [Firebase Integration](#firebase-integration)
5. [Gemini AI Integration](#gemini-ai-integration)
6. [Dependency Injection](#dependency-injection)
7. [Package Structure](#package-structure)
8. [Naming Conventions](#naming-conventions)
9. [Code Style Guidelines](#code-style-guidelines)
10. [Feature-Specific Guidelines](#feature-specific-guidelines)
11. [State Management](#state-management)
12. [Error Handling](#error-handling)
13. [Testing Guidelines](#testing-guidelines)
14. [Current Implementation Status](#current-implementation-status)

---

## Project Overview

GrowCare2 is an offline-first agricultural management application that helps
farmers and agricultural professionals with:

- **Home Dashboard**: Disease detection and assistant shortcuts
- **AI Assistant**: On-device agricultural chat for advice and guidance
- **Disease Detection**: Plant disease identification through image scanning
- **Profile Management**: User settings and preferences

### Core Features

- Real-time offline AI-powered agricultural assistance
- Image-based disease analysis
- On-device YOLO11s disease detection
- On-device Gemma 3n chat and explanation generation
- Offline-first architecture for rural connectivity
- Material3 design system with agricultural theme

### Offline-First Product Rules

- Do not introduce weather, authentication, storage, or cloud database dependencies into core app flows.
- Do not use remote Gemini or other cloud LLM APIs for disease detection or chat.
- Prefer local Room storage, local model loading, and device-side inference.
- Seed quality and fertilizer flows are out of scope for GrowCare2.

---

## Technology Stack

### Core Dependencies

```kotlin
// Kotlin & AGP
kotlin = "2.0.21"
agp = "8.13.1"

// AndroidX Core
androidx-core-ktx = "1.17.0"
androidx-lifecycle-runtime-ktx = "2.10.0"

// Jetpack Compose
compose-bom = "2024.09.00"
androidx-material3
androidx-material-icons-extended
androidx-ui
androidx-ui-graphics
androidx-ui-tooling-preview

// Navigation
androidx-navigation-compose = "2.8.0"
```

### Required Additional Dependencies

Add these to `gradle/libs.versions.toml` and `app/build.gradle.kts`:

```toml
[versions]
hilt = "2.50"
hilt-navigation-compose = "1.1.0"
firebase-bom = "32.7.0"
retrofit = "2.9.0"
okhttp = "4.12.0"
room = "2.6.1"
coil = "2.5.0"
camerax = "1.3.1"
gemini-ai = "0.1.2"
coroutines-play-services = "1.7.3"

[libraries]
# Hilt - Dependency Injection
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-storage-ktx = { group = "com.google.firebase", name = "firebase-storage-ktx" }

# Networking
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Gemini AI
generativeai = { group = "com.google.ai.client.generativeai", name = "generativeai", version.ref = "gemini-ai" }

# Room Database
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# CameraX
camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# Image Loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Coroutines
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines-play-services" }
```

### Plugins Required

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}
```

---

## Architecture Guidelines

### MVVM Architecture Layers

```
presentation/              # UI Layer
├── screens/              # Composable screens
├── viewmodels/           # ViewModels with StateFlow
├── navigation/           # Navigation configuration
└── components/           # Reusable UI components

domain/                   # Business Logic Layer
├── model/               # Domain models (clean, UI-independent)
├── usecase/             # Business logic use cases
└── repository/          # Repository interfaces

data/                    # Data Layer
├── repository/          # Repository implementations
├── remote/              # Remote data sources
│   ├── firebase/        # Firebase services
│   └── gemini/          # Gemini AI client
├── local/               # Local data sources
│   ├── database/        # Room database
│   └── preferences/     # DataStore preferences
└── mapper/              # Data <-> Domain mapping
```

### Data Flow Pattern

```
UI (Composable)
    ↓ User Action
ViewModel
    ↓ Business Logic
UseCase
    ↓ Data Request
Repository Interface
    ↓ Implementation
Repository Implementation
    ↓ Data Source Selection
DataSource (Remote/Local)
    ↓ Result
[Flow upward with callbacks/Flow/LiveData]
```

### ViewModel Implementation Pattern

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: FeatureUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI State with StateFlow
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    // Events for one-time actions
    private val _events = MutableSharedFlow<FeatureEvent>()
    val events: SharedFlow<FeatureEvent> = _events.asSharedFlow()

    // Handle user actions
    fun onAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.LoadData -> loadData()
            is FeatureAction.Submit -> submitData(action.data)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            useCase.execute()
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        data = data,
                        isLoading = false,
                        error = null
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message
                    )}
                    _events.emit(FeatureEvent.ShowError(error.message))
                }
        }
    }
}

// UI State
data class FeatureUiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// User Actions
sealed interface FeatureAction {
    data object LoadData : FeatureAction
    data class Submit(val data: String) : FeatureAction
}

// One-time Events
sealed interface FeatureEvent {
    data class ShowError(val message: String?) : FeatureEvent
    data object NavigateBack : FeatureEvent
}
```

### UseCase Pattern

```kotlin
class FeatureUseCase @Inject constructor(
    private val repository: FeatureRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(params: Params): Result<Data> = withContext(dispatcher) {
        try {
            val result = repository.getData(params)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Repository Pattern

```kotlin
// Domain layer - Interface
interface FeatureRepository {
    suspend fun getData(): Flow<List<DomainModel>>
    suspend fun saveData(data: DomainModel): Result<Unit>
}

// Data layer - Implementation
class FeatureRepositoryImpl @Inject constructor(
    private val remoteDataSource: FeatureRemoteDataSource,
    private val localDataSource: FeatureLocalDataSource,
    private val mapper: FeatureMapper
) : FeatureRepository {

    override suspend fun getData(): Flow<List<DomainModel>> = flow {
        // Try remote first
        try {
            val remoteData = remoteDataSource.fetchData()
            localDataSource.saveData(remoteData) // Cache locally
            emit(remoteData.map { mapper.toDomain(it) })
        } catch (e: Exception) {
            // Fallback to local cache
            val localData = localDataSource.getData()
            emit(localData.map { mapper.toDomain(it) })
        }
    }

    override suspend fun saveData(data: DomainModel): Result<Unit> {
        return try {
            val dataEntity = mapper.toEntity(data)
            localDataSource.saveData(dataEntity)
            remoteDataSource.uploadData(dataEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Firebase Integration

### Setup Configuration

1. **Add `google-services.json`** to `app/` directory
2. **Enable Firebase services** in Firebase Console:
   - Authentication (Email/Password, Google Sign-In)
   - Cloud Firestore
   - Storage

### Authentication Implementation

```kotlin
// data/remote/firebase/FirebaseAuthDataSource.kt
class FirebaseAuthDataSource @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user?.let {
                        AuthUser(
                            uid = it.uid,
                            email = it.email ?: "",
                            displayName = it.displayName
                        )
                    }
                    continuation.resume(Result.success(user ?: throw Exception("User is null")))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
        suspendCancellableCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user?.let {
                        AuthUser(uid = it.uid, email = it.email ?: "")
                    }
                    continuation.resume(Result.success(user ?: throw Exception("User is null")))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    fun getCurrentUser(): AuthUser? {
        return auth.currentUser?.let {
            AuthUser(
                uid = it.uid,
                email = it.email ?: "",
                displayName = it.displayName
            )
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
```

### Firestore Data Management

```kotlin
// data/remote/firebase/FirestoreDataSource.kt
class FirestoreDataSource @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveDiseaseAnalysis(userId: String, analysisData: DiseaseAnalysisEntity): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            db.collection("users")
                .document(userId)
                .collection("disease_analyses")
                .document(analysisData.id)
                .set(analysisData)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    fun getDiseaseAnalysisStream(userId: String): Flow<List<DiseaseAnalysisEntity>> = callbackFlow {
        val listener = db.collection("users")
            .document(userId)
            .collection("disease_analyses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val analyses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DiseaseAnalysisEntity::class.java)
                } ?: emptyList()

                trySend(analyses)
            }

        awaitClose { listener.remove() }
    }
}
}
```

### Firebase Storage for Images

```kotlin
// data/remote/firebase/FirebaseStorageDataSource.kt
class FirebaseStorageDataSource @Inject constructor() {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadImage(
        userId: String,
        imageUri: Uri,
        path: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val ref = storage.reference
            .child("users/$userId/$path/${System.currentTimeMillis()}.jpg")

        ref.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                continuation.resume(Result.success(downloadUrl.toString()))
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }
}
```

---

## Gemini AI Integration

### API Setup

1. **Get API Key** from Google AI Studio:
   https://makersuite.google.com/app/apikey
2. **Store securely** in `local.properties`:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
3. **Access in build.gradle.kts**:
   ```kotlin
   android {
       defaultConfig {
           val geminiApiKey = project.findProperty("GEMINI_API_KEY") as String? ?: ""
           buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
       }
       buildFeatures {
           buildConfig = true
       }
   }
   ```

### Gemini Client Implementation

```kotlin
// data/remote/gemini/GeminiClient.kt
class GeminiClient @Inject constructor() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun sendMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(message)
            Result.success(response.text ?: "No response")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeImage(
        imageUri: Uri,
        prompt: String,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, imageUri)
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            Result.success(response.text ?: "No analysis available")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatStream(messages: List<ChatMessage>): Flow<String> = flow {
        val chat = generativeModel.startChat(
            history = messages.dropLast(1).map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.content)
                }
            }
        )

        val lastMessage = messages.lastOrNull()?.content ?: return@flow

        chat.sendMessageStream(lastMessage).collect { chunk ->
            emit(chunk.text ?: "")
        }
    }
}
```

### AI Use Cases

```kotlin
// domain/usecase/chat/SendChatMessageUseCase.kt
class SendChatMessageUseCase @Inject constructor(
    private val geminiClient: GeminiClient,
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        message: String,
        conversationId: String
    ): Flow<ChatMessage> = flow {
        // Save user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        chatRepository.saveMessage(conversationId, userMessage)
        emit(userMessage)

        // Get AI response
        val aiResponseBuilder = StringBuilder()
        geminiClient.getChatStream(chatRepository.getMessages(conversationId))
            .collect { chunk ->
                aiResponseBuilder.append(chunk)
                emit(ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = aiResponseBuilder.toString(),
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isStreaming = true
                ))
            }

        // Save final AI message
        val aiMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = aiResponseBuilder.toString(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isStreaming = false
        )
        chatRepository.saveMessage(conversationId, aiMessage)
        emit(aiMessage)
    }
}

// domain/usecase/detection/AnalyzePlantDiseaseUseCase.kt
class AnalyzePlantDiseaseUseCase @Inject constructor(
    private val geminiClient: GeminiClient,
    private val context: Context
) {
    suspend operator fun invoke(imageUri: Uri): Result<DiseaseAnalysis> {
        val prompt = """
            Analyze this plant image and identify any diseases or health issues.
            Provide:
            1. Disease name (if any)
            2. Confidence level (0-100%)
            3. Symptoms observed
            4. Recommended treatment
            5. Prevention measures

            Format the response as JSON with keys: diseaseName, confidence, symptoms, treatment, prevention
        """.trimIndent()

        return geminiClient.analyzeImage(imageUri, prompt, context)
            .map { response ->
                // Parse JSON response
                parseDiseaseAnalysis(response)
            }
    }

    private fun parseDiseaseAnalysis(jsonResponse: String): DiseaseAnalysis {
        // Parse JSON and return structured data
        // Implementation depends on response format
        return DiseaseAnalysis(/* ... */)
    }
}
```

---

## Dependency Injection

### Hilt Setup

```kotlin
// Application class
@HiltAndroidApp
class GrowCareApplication : Application()

// MainActivity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
    }
}
```

### Module Examples

```kotlin
// di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }
}

// di/FirebaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "growcare_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDiseaseAnalysisDao(database: AppDatabase): DiseaseAnalysisDao = database.diseaseAnalysisDao()
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
}
```

---

## Package Structure

```
com.example.mobileappdev/
├── GrowCareApplication.kt
├── MainActivity.kt
│
├── di/                                    # Dependency Injection
│   ├── AppModule.kt
│   ├── FirebaseModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
│
├── presentation/                          # UI Layer
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   │
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   │   ├── LoginScreen.kt
│   │   │   │   ├── LoginViewModel.kt
│   │   │   │   └── LoginUiState.kt
│   │   │   └── signup/
│   │   │       ├── SignUpScreen.kt
│   │   │       ├── SignUpViewModel.kt
│   │   │       └── SignUpUiState.kt
│   │   │
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── components/
│   │   │       ├── WeatherCard.kt
│   │   │       └── QuickActionGrid.kt
│   │   │
│   │   ├── chat/
│   │   │   ├── ChatScreen.kt
│   │   │   ├── ChatViewModel.kt
│   │   │   └── components/
│   │   │       ├── MessageBubble.kt
│   │   │       └── ChatInput.kt
│   │   │
│   │   ├── fertilizer/
│   │   │   ├── FertilizerScreen.kt
│   │   │   ├── FertilizerViewModel.kt
│   │   │   └── FertilizerResult.kt
│   │   │
│   │   ├── detection/
│   │   │   ├── DiseaseScanScreen.kt
│   │   │   ├── DiseaseResultScreen.kt
│   │   │   └── DiseaseViewModel.kt
│   │   │
│   │   ├── seed/
│   │   │   ├── SeedScanScreen.kt
│   │   │   ├── SeedResultScreen.kt
│   │   │   └── SeedViewModel.kt
│   │   │
│   │   └── profile/
│   │       ├── ProfileScreen.kt
│   │       └── ProfileViewModel.kt
│   │
│   ├── components/                        # Shared UI Components
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorMessage.kt
│   │   ├── PrimaryButton.kt
│   │   └── ImagePicker.kt
│   │
│   └── ui/
│       └── theme/
│           ├── Color.kt
│           ├── Theme.kt
│           └── Type.kt
│
├── domain/                                # Business Logic Layer
│   ├── model/
│   │   ├── User.kt
│   │   ├── ChatMessage.kt
│   │   ├── DiseaseAnalysis.kt
│   │   ├── FertilizerRecommendation.kt
│   │   └── SeedQuality.kt
│   │
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── DiseaseRepository.kt
│   │   └── UserRepository.kt
│   │
│   └── usecase/
│       ├── auth/
│       │   ├── SignInUseCase.kt
│       │   ├── SignUpUseCase.kt
│       │   └── SignOutUseCase.kt
│       ├── chat/
│       │   ├── SendChatMessageUseCase.kt
│       │   └── GetChatHistoryUseCase.kt
│       ├── detection/
│       │   ├── AnalyzePlantDiseaseUseCase.kt
│       │   └── AnalyzeSeedQualityUseCase.kt
│       └── fertilizer/
│           └── CalculateFertilizerUseCase.kt
│
└── data/                                  # Data Layer
    ├── repository/                        # Repository Implementations
    │   ├── AuthRepositoryImpl.kt
    │   ├── ChatRepositoryImpl.kt
    │   ├── DiseaseRepositoryImpl.kt
    │   └── UserRepositoryImpl.kt
    │
    ├── remote/                            # Remote Data Sources
    │   ├── firebase/
    │   │   ├── FirebaseAuthDataSource.kt
    │   │   ├── FirestoreDataSource.kt
    │   │   └── FirebaseStorageDataSource.kt
    │   └── gemini/
    │       └── GeminiClient.kt
    │
    ├── local/                             # Local Data Sources
    │   ├── database/
    │   │   ├── AppDatabase.kt
    │   │   ├── dao/
    │   │   │   ├── ChatDao.kt
    │   │   │   ├── DiseaseAnalysisDao.kt
    │   │   │   └── UserDao.kt
    │   │   └── entity/
    │   │       ├── ChatMessageEntity.kt
    │   │       ├── DiseaseAnalysisEntity.kt
    │   │       └── UserEntity.kt
    │   └── preferences/
    │       └── UserPreferences.kt
    │
    └── mapper/                            # Data <-> Domain Mappers
        ├── ChatMapper.kt
        ├── DiseaseMapper.kt
        └── UserMapper.kt
```

---

## Naming Conventions

### Files

- **Screens**: `[Feature]Screen.kt` (e.g., `LoginScreen.kt`, `HomeScreen.kt`)
- **ViewModels**: `[Feature]ViewModel.kt` (e.g., `LoginViewModel.kt`)
- **Repositories**: `[Feature]Repository.kt` / `[Feature]RepositoryImpl.kt`
- **Use Cases**: `[Action][Feature]UseCase.kt` (e.g.,
  `SendChatMessageUseCase.kt`)
- **Data Sources**: `[Source][Feature]DataSource.kt` (e.g.,
  `FirebaseAuthDataSource.kt`)
- **Entities**: `[Feature]Entity.kt` (e.g., `DiseaseAnalysisEntity.kt`)
- **DAOs**: `[Feature]Dao.kt` (e.g., `DiseaseAnalysisDao.kt`)
- **Mappers**: `[Feature]Mapper.kt` (e.g., `DiseaseMapper.kt`)

### Classes & Interfaces

- **Composables**: PascalCase (e.g., `MessageBubble`, `WeatherCard`)
- **ViewModels**: `[Feature]ViewModel`
- **Repositories**: Interface without suffix, Implementation with `Impl`
- **Use Cases**: Descriptive action name ending in `UseCase`
- **Data Classes**: PascalCase (e.g., `ChatMessage`, `UserProfile`)

### Variables & Functions

- **Variables**: camelCase (e.g., `userName`, `analysisData`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_IMAGE_SIZE`, `API_TIMEOUT`)
- **Functions**: camelCase starting with verb (e.g., `loadUserData()`,
  `calculateNPK()`)
- **Composables**: PascalCase (e.g., `LoginButton()`, `ProfileHeader()`)
- **Boolean variables**: Start with `is`, `has`, `should` (e.g., `isLoading`,
  `hasError`)

### Resources

- **Drawables**: `snake_case.xml` or `.png` (e.g., `ic_plant.xml`,
  `bg_gradient.xml`)
- **Layouts**: Not used (100% Compose)
- **Strings**: `snake_case` (e.g., `app_name`, `error_invalid_email`)
- **Colors**: Descriptive names in `Color.kt` (e.g., `PrimaryGreen`,
  `BackgroundGray`)
- **Dimensions**: `snake_case` (e.g., `padding_small`, `icon_size_large`)

---

## Code Style Guidelines

### Jetpack Compose Best Practices

#### 1. Composable Structure

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeatureEvent.NavigateBack -> onNavigateBack()
                is FeatureEvent.ShowError -> {
                    // Show error snackbar
                }
            }
        }
    }

    FeatureContent(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun FeatureContent(
    uiState: FeatureUiState,
    onAction: (FeatureAction) -> Unit
) {
    Scaffold(
        topBar = { FeatureTopBar(onAction) },
        content = { padding ->
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorMessage(uiState.error)
                else -> FeatureList(uiState.data, onAction)
            }
        }
    )
}
```

#### 2. State Management

```kotlin
// ✅ DO: Use StateFlow in ViewModel
@HiltViewModel
class FeatureViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()
}

// ✅ DO: Collect state in Composable
@Composable
fun MyScreen(viewModel: FeatureViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}

// ❌ DON'T: Use mutableStateOf in ViewModel
class FeatureViewModel : ViewModel() {
    var uiState by mutableStateOf(FeatureUiState()) // ❌ Wrong!
}
```

#### 3. Side Effects

```kotlin
// Use LaunchedEffect for one-time actions
LaunchedEffect(key1 = userId) {
    viewModel.loadUserData(userId)
}

// Use DisposableEffect for cleanup
DisposableEffect(Unit) {
    val listener = registerListener()
    onDispose {
        listener.remove()
    }
}

// Use rememberCoroutineScope for event handlers
val scope = rememberCoroutineScope()
Button(onClick = {
    scope.launch {
        viewModel.performAction()
    }
}) {
    Text("Action")
}
```

#### 4. Reusable Components

```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
```

### Kotlin Conventions

```kotlin
// ✅ DO: Use data classes for models
data class DiseaseAnalysis(
    val id: String,
    val diseaseName: String,
    val confidence: Float,
    val detectedDate: Long
)

// ✅ DO: Use sealed classes for states
sealed interface LoadingState {
    data object Idle : LoadingState
    data object Loading : LoadingState
    data class Success<T>(val data: T) : LoadingState
    data class Error(val message: String) : LoadingState
}

// ✅ DO: Use extension functions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// ✅ DO: Use scope functions appropriately
fun processUser(user: User?) {
    user?.let {
        saveToDatabase(it)
        notifyUI(it)
    }
}

// ✅ DO: Use nullable types explicitly
fun findUser(id: String): User? {
    return database.query(id)
}
```

---

## Feature-Specific Guidelines

### Home Dashboard

```kotlin
// Features:
// - Weather display (integrate weather API)
// - Quick action buttons (navigation to features)
// - Recent activity feed

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFeature: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn {
        item { WeatherCard(uiState.weather) }
        item { QuickActionGrid(onNavigateToFeature) }
        item { RecentActivitySection(uiState.activities) }
    }
}
```

### AI Chat Assistant

```kotlin
// Features:
// - Streaming responses from Gemini
// - Message history persistence
// - Image attachment support
// - Agricultural context awareness

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }

        ChatInput(
            onSendMessage = viewModel::sendMessage,
            isLoading = isLoading
        )
    }
}

// ViewModel
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendChatMessageUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            sendMessageUseCase(text, conversationId = "default")
                .collect { message ->
                    _messages.update { it + message }
                }
        }
    }
}
```

### Fertilizer Calculator

```kotlin
// Features:
// - Input: soil type, area, current NPK
// - Calculate NPK requirements
// - Recommend fertilizer products
// - Cost estimation

data class FertilizerInput(
    val soilType: String,
    val area: Double, // in acres
    val currentNPK: NPK,
    val targetYield: Double
)

data class NPK(
    val nitrogen: Double,
    val phosphorus: Double,
    val potassium: Double
)

data class FertilizerRecommendation(
    val requiredNPK: NPK,
    val products: List<FertilizerProduct>,
    val estimatedCost: Double,
    val applicationSchedule: List<ApplicationPhase>
)
```

### Disease Detection

```kotlin
// Features:
// - Camera capture or gallery selection
// - Image preprocessing
// - Gemini Vision analysis
// - Disease identification
// - Treatment recommendations

@HiltViewModel
class DiseaseViewModel @Inject constructor(
    private val analyzeDisease: AnalyzePlantDiseaseUseCase,
    private val storageDataSource: FirebaseStorageDataSource
) : ViewModel() {

    fun analyzePlantImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }

            // Upload to storage
            storageDataSource.uploadImage(
                userId = getCurrentUserId(),
                imageUri = imageUri,
                path = "disease_scans"
            ).onSuccess { downloadUrl ->
                // Analyze with Gemini
                analyzeDisease(imageUri)
                    .onSuccess { analysis ->
                        _uiState.update { it.copy(
                            isAnalyzing = false,
                            result = analysis,
                            imageUrl = downloadUrl
                        )}
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(
                            isAnalyzing = false,
                            error = error.message
                        )}
                    }
            }
        }
    }
}
```

### Seed Quality Scanner

```kotlin
// Features:
// - Capture seed image
// - AI analysis of seed quality
// - Metrics: size, color, damage, germination potential
// - Quality score and recommendations

data class SeedQualityAnalysis(
    val qualityScore: Int, // 0-100
    val size: SeedSize,
    val color: ColorConsistency,
    val damage: DamageAssessment,
    val germinationPotential: Int, // 0-100
    val recommendations: List<String>
)

enum class SeedSize { SMALL, MEDIUM, LARGE, MIXED }
enum class ColorConsistency { UNIFORM, SLIGHTLY_VARIED, HIGHLY_VARIED }
data class DamageAssessment(
    val percentage: Int,
    val types: List<DamageType>
)
enum class DamageType { INSECT, FUNGAL, MECHANICAL, NONE }
```

---

## State Management

### UI State Pattern

```kotlin
// Single source of truth
data class FeatureUiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedItem: Item? = null,
    val filterType: FilterType = FilterType.ALL
)

// Update state immutably
_uiState.update { currentState ->
    currentState.copy(
        isLoading = false,
        data = newData
    )
}
```

### Loading States

```kotlin
sealed interface LoadingState<out T> {
    data object Idle : LoadingState<Nothing>
    data object Loading : LoadingState<Nothing>
    data class Success<T>(val data: T) : LoadingState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : LoadingState<Nothing>
}

// Usage in ViewModel
private val _diseaseState = MutableStateFlow<LoadingState<List<DiseaseAnalysis>>>(LoadingState.Idle)
val diseaseState: StateFlow<LoadingState<List<DiseaseAnalysis>>> = _diseaseState.asStateFlow()

// In Composable
when (val state = uiState.diseaseState) {
    LoadingState.Idle -> { /* Initial state */ }
    LoadingState.Loading -> LoadingIndicator()
    is LoadingState.Success -> DiseaseList(state.data)
    is LoadingState.Error -> ErrorMessage(state.message)
}
```

---

## Error Handling

### Repository Error Handling

```kotlin
class DiseaseRepositoryImpl @Inject constructor(
    private val remoteDataSource: DiseaseRemoteDataSource,
    private val localDataSource: DiseaseLocalDataSource
) : DiseaseRepository {

    override suspend fun getDiseaseAnalyses(): Result<List<DiseaseAnalysis>> = try {
        val analyses = remoteDataSource.fetchAnalyses()
        localDataSource.saveAnalyses(analyses)
        Result.success(analyses)
    } catch (e: IOException) {
        // Network error, try cache
        val cachedAnalyses = localDataSource.getAnalyses()
        if (cachedAnalyses.isNotEmpty()) {
            Result.success(cachedAnalyses)
        } else {
            Result.failure(NetworkException("No internet and no cached data"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Custom Exceptions

```kotlin
sealed class AppException(message: String) : Exception(message) {
    class NetworkException(message: String = "Network error") : AppException(message)
    class AuthException(message: String = "Authentication failed") : AppException(message)
    class ValidationException(message: String = "Validation failed") : AppException(message)
    class NotFoundException(message: String = "Resource not found") : AppException(message)
    class ServerException(message: String = "Server error") : AppException(message)
}
```

### Error Display

```kotlin
@Composable
fun ErrorMessage(
    message: String?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (message != null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            onRetry?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = it) {
                    Text("Retry")
                }
            }
        }
    }
}
```

---

## Testing Guidelines

### Unit Testing ViewModels

```kotlin
@ExperimentalCoroutinesTest
class FeatureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: FeatureViewModel
    private lateinit var mockUseCase: FeatureUseCase

    @Before
    fun setup() {
        mockUseCase = mockk()
        viewModel = FeatureViewModel(mockUseCase)
    }

    @Test
    fun `loadData should update state to loading then success`() = runTest {
        // Given
        val expectedData = listOf(Item("1"), Item("2"))
        coEvery { mockUseCase() } returns Result.success(expectedData)

        // When
        viewModel.loadData()

        // Then
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(expectedData, viewModel.uiState.value.data)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `loadData should handle error`() = runTest {
        // Given
        val error = Exception("Network error")
        coEvery { mockUseCase() } returns Result.failure(error)

        // When
        viewModel.loadData()

        // Then
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("Network error", viewModel.uiState.value.error)
    }
}
```

### Composable Testing

```kotlin
@Test
fun loginButton_whenClicked_callsViewModel() {
    var clickCount = 0
    composeTestRule.setContent {
        LoginButton(onClick = { clickCount++ })
    }

    composeTestRule
        .onNodeWithText("Login")
        .performClick()

    assertEquals(1, clickCount)
}

@Test
fun errorMessage_whenErrorExists_isDisplayed() {
    composeTestRule.setContent {
        ErrorMessage(message = "Test error")
    }

    composeTestRule
        .onNodeWithText("Test error")
        .assertIsDisplayed()
}
```

---

## Current Implementation Status

### ✅ Completed

1. **UI Layer (100% Jetpack Compose)**

   - HomeScreen with weather and quick actions
   - ChatScreen with message list UI
   - FertilizerScreen with input form
   - SeedScanScreen with camera placeholder
   - DiseaseScanScreen structure
   - ProfileScreen with user info
   - LoginScreen and SignUpScreen
   - Navigation setup with NavHost

2. **Theme & Design**

   - Material3 integration
   - Color scheme (PrimaryGreen: #4CAF50)
   - Consistent styling across screens
   - Reusable UI components

3. **Build Configuration**
   - Kotlin 2.0.21
   - Compose BOM 2024.09.00
   - Gradle setup
   - Target SDK 36

### 🚧 In Progress / Needs Implementation

1. **Architecture Layer**

   - [ ] Implement ViewModels with actual logic (currently empty stubs)
   - [ ] Create Repository layer
   - [ ] Add UseCase classes for business logic
   - [ ] Set up Hilt for dependency injection

2. **Data Layer**

   - [ ] Firebase Authentication integration
   - [ ] Firestore database setup
   - [ ] Firebase Storage for images
   - [ ] Room database for offline storage
   - [ ] Data mappers (Entity ↔ Domain)

3. **AI Integration**

   - [ ] Gemini API client setup
   - [ ] Chat streaming implementation
   - [ ] Image analysis for disease detection
   - [ ] Seed quality analysis with AI
   - [ ] Agricultural knowledge base integration

4. **Features**

   - [ ] Camera integration (CameraX)
   - [ ] Image upload and processing
   - [ ] Authentication logic (login/signup)
   - [ ] Profile editing
   - [ ] Weather API integration
   - [ ] Fertilizer calculation algorithms
   - [ ] Disease detection backend
   - [ ] Seed quality analysis backend

5. **State Management**

   - [ ] Replace local state with StateFlow in ViewModels
   - [ ] Implement proper loading/error states
   - [ ] Add form validation
   - [ ] Implement offline-first architecture

6. **Testing**
   - [ ] Unit tests for ViewModels
   - [ ] Unit tests for UseCases
   - [ ] Unit tests for Repositories
   - [ ] Composable UI tests
   - [ ] Integration tests

### 📋 TODOs Found in Code

- `LoginScreen.kt`: "Implement actual login logic"
- `SignUpScreen.kt`: "Implement actual registration logic"
- `ChatScreen.kt`: "Simulate AI response - In real app, this would be an API
  call"
- `HomeScreen.kt`: "Integrate real weather API"
- `SeedScanScreen.kt`: "Implement camera capture"
- `DiseaseScanScreen.kt`: "Add image upload functionality"
- `ProfileScreen.kt`: "Implement profile editing"
- Various navigation TODOs for result screens

---

## Development Workflow

### Branch Strategy

- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Individual features
- `bugfix/*`: Bug fixes
- `hotfix/*`: Production hotfixes

### Git Commit Conventions

```
feat: Add disease detection feature
fix: Resolve chat message duplicate issue
refactor: Restructure repository layer
docs: Update Firebase setup instructions
test: Add ViewModel unit tests
chore: Update dependencies
```

### Pull Request Checklist

- [ ] Code follows MVVM architecture
- [ ] All ViewModels use StateFlow
- [ ] Proper error handling implemented
- [ ] No hardcoded strings (use strings.xml)
- [ ] Composables are reusable where applicable
- [ ] Unit tests added for business logic
- [ ] No lint errors
- [ ] Firebase rules updated if needed
- [ ] API keys secured in local.properties

---

## Additional Resources

### Documentation

- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Firebase Android Docs](https://firebase.google.com/docs/android/setup)
- [Gemini API Docs](https://ai.google.dev/docs)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Material 3 Design](https://m3.material.io/)

### Code Examples

For implementation examples, refer to:

- `presentation/screens/home/HomeScreen.kt` - Compose structure
- `ui/theme/Color.kt` - Color definitions
- `MainActivity.kt` - Navigation setup

---

## Quick Reference

### Color Palette

```kotlin
val PrimaryGreen = Color(0xFF4CAF50)
val PrimaryBlue = Color(0xFF2196F3)
val BackgroundGray = Color(0xFFF5F5F5)
val TextBlack = Color(0xFF1A1A1A)
val TextGray = Color(0xFF757575)
val White = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFF44336)
```

### Common Dimensions

```kotlin
val PaddingSmall = 8.dp
val PaddingMedium = 16.dp
val PaddingLarge = 24.dp
val CornerRadiusSmall = 8.dp
val CornerRadiusMedium = 12.dp
val IconSizeSmall = 24.dp
val IconSizeMedium = 32.dp
val ButtonHeight = 56.dp
```

### Navigation Routes

```kotlin
object Screen {
    const val HOME = "home"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val CHAT = "chat"
    const val FERTILIZER = "fertilizer"
    const val SEED_SCAN = "seed_scan"
    const val DISEASE_SCAN = "disease_scan"
    const val PROFILE = "profile"
}
```

---

**Last Updated**: December 9, 2025  
**Version**: 1.0.0  
**Maintainer**: GrowCare Development Team

For questions or clarifications, refer to this document or consult with the team
lead.
