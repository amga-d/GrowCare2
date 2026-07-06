package com.example.growCare.presentation.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.growCare.domain.model.User
import com.example.growCare.domain.usecase.user.GetUserProfileUseCase
import com.example.growCare.domain.usecase.user.UpdateUserProfileUseCase
import com.example.growCare.domain.usecase.user.UploadProfilePictureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val farmSize: String = "",
    val profilePictureUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val validationError: String? = null,
    val hasChanges: Boolean = false,
    val originalUser: User? = null
)

sealed interface EditProfileAction {
    data class UpdateDisplayName(val name: String) : EditProfileAction
    data class UpdatePhoneNumber(val phone: String) : EditProfileAction
    data class UpdateLocation(val location: String) : EditProfileAction
    data class UpdateFarmSize(val size: String) : EditProfileAction
    data class SelectImage(val uri: Uri) : EditProfileAction
    data object RemoveImage : EditProfileAction
    data object SaveProfile : EditProfileAction
}

sealed interface EditProfileEvent {
    data class ShowError(val message: String) : EditProfileEvent
    data object SaveSuccess : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val saveUserProfileUseCase: com.example.growCare.domain.usecase.user.SaveUserProfileUseCase,
    private val uploadProfilePictureUseCase: UploadProfilePictureUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProfileEvent>()
    val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        loadUserProfile()
    }

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.UpdateDisplayName -> updateDisplayName(action.name)
            is EditProfileAction.UpdatePhoneNumber -> updatePhoneNumber(action.phone)
            is EditProfileAction.UpdateLocation -> updateLocation(action.location)
            is EditProfileAction.UpdateFarmSize -> updateFarmSize(action.size)
            is EditProfileAction.SelectImage -> selectImage(action.uri)
            EditProfileAction.RemoveImage -> removeImage()
            EditProfileAction.SaveProfile -> saveProfile()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                // Get user profile once, not continuously
                val user = getUserProfileUseCase().first()
                
                if (user != null) {
                    println("EditProfile: Loaded user - email=${user.email}, name=${user.displayName}")
                    _uiState.update { state ->
                        state.copy(
                            email = user.email,
                            displayName = user.displayName ?: "",
                            phoneNumber = user.phoneNumber ?: "",
                            location = user.location ?: "",
                            farmSize = user.farmSize?.toString() ?: "",
                            profilePictureUrl = user.profilePictureUrl,
                            originalUser = user,
                            hasChanges = false
                        )
                    }
                } else {
                    println("EditProfile: No user found, creating local profile")
                    createLocalProfile()
                }
            } catch (e: Exception) {
                println("EditProfile: Error loading profile - ${e.message}")
                createLocalProfile()
            }
        }
    }
    
    private fun createLocalProfile() {
        viewModelScope.launch {
            try {
                val newUser = com.example.growCare.domain.model.User(
                    uid = "local-user",
                    email = "local@growcare2",
                    displayName = "Farmer",
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )

                println("EditProfile: Creating local profile - ${newUser.email}")

                saveUserProfileUseCase(newUser)
                    .onSuccess {
                        println("EditProfile: Local profile created successfully")
                    }
                    .onFailure { e ->
                        println("EditProfile: Failed to create local profile - ${e.message}")
                        _events.emit(EditProfileEvent.ShowError("Could not create local profile: ${e.message}"))
                    }
            } catch (e: Exception) {
                println("EditProfile: Exception creating local profile - ${e.message}")
                _events.emit(EditProfileEvent.ShowError("Error loading profile: ${e.message}"))
            }
        }
    }

    private fun updateDisplayName(name: String) {
        _uiState.update { state ->
            val hasChanges = name != (state.originalUser?.displayName ?: "") ||
                    state.phoneNumber != (state.originalUser?.phoneNumber ?: "") ||
                    state.location != (state.originalUser?.location ?: "") ||
                    state.farmSize != (state.originalUser?.farmSize?.toString() ?: "") ||
                    state.profilePictureUrl != state.originalUser?.profilePictureUrl
            println("EditProfile: updateDisplayName - name='$name', original='${state.originalUser?.displayName}', hasChanges=$hasChanges")
            state.copy(displayName = name, hasChanges = hasChanges)
        }
    }

    private fun updatePhoneNumber(phone: String) {
        _uiState.update { state ->
            val hasChanges = state.displayName != (state.originalUser?.displayName ?: "") ||
                    phone != (state.originalUser?.phoneNumber ?: "") ||
                    state.location != (state.originalUser?.location ?: "") ||
                    state.farmSize != (state.originalUser?.farmSize?.toString() ?: "") ||
                    state.profilePictureUrl != state.originalUser?.profilePictureUrl
            state.copy(phoneNumber = phone, hasChanges = hasChanges)
        }
    }

    private fun updateLocation(location: String) {
        _uiState.update { state ->
            val hasChanges = state.displayName != (state.originalUser?.displayName ?: "") ||
                    state.phoneNumber != (state.originalUser?.phoneNumber ?: "") ||
                    location != (state.originalUser?.location ?: "") ||
                    state.farmSize != (state.originalUser?.farmSize?.toString() ?: "") ||
                    state.profilePictureUrl != state.originalUser?.profilePictureUrl
            state.copy(location = location, hasChanges = hasChanges)
        }
    }

    private fun updateFarmSize(size: String) {
        _uiState.update { state ->
            val hasChanges = state.displayName != (state.originalUser?.displayName ?: "") ||
                    state.phoneNumber != (state.originalUser?.phoneNumber ?: "") ||
                    state.location != (state.originalUser?.location ?: "") ||
                    size != (state.originalUser?.farmSize?.toString() ?: "") ||
                    state.profilePictureUrl != state.originalUser?.profilePictureUrl
            state.copy(farmSize = size, hasChanges = hasChanges)
        }
    }

    private fun selectImage(uri: Uri) {
        _uiState.update { it.copy(selectedImageUri = uri) }
        uploadProfilePicture(uri)
    }

    private fun removeImage() {
        _uiState.update { state ->
            val hasChanges = state.displayName != (state.originalUser?.displayName ?: "") ||
                    state.phoneNumber != (state.originalUser?.phoneNumber ?: "") ||
                    state.location != (state.originalUser?.location ?: "") ||
                    state.farmSize != (state.originalUser?.farmSize?.toString() ?: "") ||
                    null != state.originalUser?.profilePictureUrl
            state.copy(
                profilePictureUrl = null,
                selectedImageUri = null,
                hasChanges = hasChanges
            )
        }
    }

    private fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }

            uploadProfilePictureUseCase(uri)
                .onSuccess { downloadUrl ->
                    _uiState.update { state ->
                        val hasChanges = state.displayName != (state.originalUser?.displayName ?: "") ||
                                state.phoneNumber != (state.originalUser?.phoneNumber ?: "") ||
                                state.location != (state.originalUser?.location ?: "") ||
                                state.farmSize != (state.originalUser?.farmSize?.toString() ?: "") ||
                                downloadUrl != state.originalUser?.profilePictureUrl
                        state.copy(
                            profilePictureUrl = downloadUrl,
                            isUploadingImage = false,
                            hasChanges = hasChanges
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUploadingImage = false) }
                    _events.emit(
                        EditProfileEvent.ShowError(
                            e.message ?: "Failed to upload image"
                        )
                    )
                }
        }
    }

    private fun saveProfile() {
        println("EditProfile: saveProfile called")
        
        if (!validateInput()) {
            println("EditProfile: validation failed")
            return
        }

        val currentState = _uiState.value
        if (currentState.originalUser == null) {
            println("EditProfile: originalUser is null, cannot save")
            viewModelScope.launch {
                _events.emit(EditProfileEvent.ShowError("Profile not loaded. Please try again."))
            }
            return
        }
        
        val originalUser = currentState.originalUser

        val updatedUser = originalUser.copy(
            displayName = currentState.displayName.takeIf { it.isNotBlank() },
            phoneNumber = currentState.phoneNumber.takeIf { it.isNotBlank() },
            location = currentState.location.takeIf { it.isNotBlank() },
            farmSize = currentState.farmSize.toDoubleOrNull(),
            profilePictureUrl = currentState.profilePictureUrl
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }

            println("EditProfile: Saving profile - user=${updatedUser.email}, name=${updatedUser.displayName}")
            updateUserProfileUseCase(updatedUser)
                .onSuccess {
                    println("EditProfile: Save successful")
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(EditProfileEvent.SaveSuccess)
                }
                .onFailure { e ->
                    println("EditProfile: Save failed - ${e.message}")
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(
                        EditProfileEvent.ShowError(
                            e.message ?: "Failed to save profile"
                        )
                    )
                }
        }
    }

    private fun validateInput(): Boolean {
        val currentState = _uiState.value

        if (currentState.displayName.isBlank()) {
            println("EditProfile: validation error - display name is blank")
            _uiState.update { it.copy(validationError = "Display name cannot be empty") }
            viewModelScope.launch {
                _events.emit(EditProfileEvent.ShowError("Display name cannot be empty"))
            }
            return false
        }

        if (currentState.farmSize.isNotBlank()) {
            val farmSizeValue = currentState.farmSize.toDoubleOrNull()
            if (farmSizeValue == null || farmSizeValue < 0) {
                println("EditProfile: validation error - invalid farm size")
                _uiState.update { it.copy(validationError = "Farm size must be a valid positive number") }
                viewModelScope.launch {
                    _events.emit(EditProfileEvent.ShowError("Farm size must be a valid positive number"))
                }
                return false
            }
        }

        _uiState.update { it.copy(validationError = null) }
        println("EditProfile: validation passed")
        return true
    }

    private fun checkForChanges() {
        val currentState = _uiState.value
        val originalUser = currentState.originalUser ?: return

        val hasChanges = currentState.displayName != (originalUser.displayName ?: "") ||
                currentState.phoneNumber != (originalUser.phoneNumber ?: "") ||
                currentState.location != (originalUser.location ?: "") ||
                currentState.farmSize != (originalUser.farmSize?.toString() ?: "") ||
                currentState.profilePictureUrl != originalUser.profilePictureUrl

        _uiState.update { it.copy(hasChanges = hasChanges) }
    }
}
