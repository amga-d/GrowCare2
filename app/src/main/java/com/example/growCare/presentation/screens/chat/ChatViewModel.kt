package com.example.growCare.presentation.screens.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.growCare.domain.model.ChatMessage
import com.example.growCare.domain.usecase.chat.GetAllConversationsUseCase
import com.example.growCare.domain.usecase.chat.GetChatHistoryUseCase
import com.example.growCare.domain.usecase.chat.SendChatMessageUseCase
import com.example.growCare.domain.usecase.chat.SendMessageWithImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI Chat feature
 * Handles message sending, streaming responses, and chat history
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val sendMessageWithImageUseCase: SendMessageWithImageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val getAllConversationsUseCase: GetAllConversationsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(ChatUiState(
        conversationId = "chat_${System.currentTimeMillis()}"
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Events for one-time actions
    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    init {
        val conversationId = savedStateHandle.get<String>("conversationId")
        if (conversationId != null) {
            loadConversation(conversationId)
        } else {
            loadChatHistory()
        }
    }

    /**
     * Handle user actions
     */
    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendMessage -> sendMessage(action.message)
            is ChatAction.SendMessageWithImage -> sendMessageWithImage(action.message, action.imageUri)
            ChatAction.ClearChat -> clearChat()
            ChatAction.RetryLastMessage -> retryLastMessage()
            ChatAction.StartNewChat -> startNewChat()
            is ChatAction.LoadConversation -> loadConversation(action.conversationId)
            ChatAction.LoadAllConversations -> loadAllConversations()
        }
    }

    /**
     * Load chat history from repository
     */
    private fun loadChatHistory() {
        viewModelScope.launch {
            getChatHistoryUseCase(_uiState.value.conversationId).collect { messages ->
                _uiState.update { it.copy(
                    messages = messages,
                    isLoading = false
                )}
            }
        }
    }

    /**
     * Send text message to AI
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        // Prevent sending if already sending
        if (_uiState.value.isSending) return

        _uiState.update { it.copy(
            isSending = true,
            error = null
        )}

        viewModelScope.launch {
            try {
                sendChatMessageUseCase(message, _uiState.value.conversationId).collect { chatMessage ->
                    // Update or add message in the list
                    _uiState.update { state ->
                        val existingMessages = state.messages.toMutableList()
                        
                        // Check if this message already exists (for streaming updates)
                        val existingIndex = existingMessages.indexOfFirst { it.id == chatMessage.id }
                        
                        if (existingIndex >= 0) {
                            // Update existing message
                            existingMessages[existingIndex] = chatMessage
                        } else {
                            // Add new message
                            existingMessages.add(chatMessage)
                        }
                        
                        state.copy(
                            messages = existingMessages,
                            isSending = chatMessage.isStreaming,
                            error = null
                        )
                    }

                    // Scroll to bottom when new message arrives
                    if (!chatMessage.isStreaming) {
                        _events.emit(ChatEvent.ScrollToBottom)
                    }
                }
                // Ensure isSending is false after collection completes
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send message"
                )}
                _events.emit(ChatEvent.ShowError(e.message ?: "Failed to send message"))
            }
        }
    }

    /**
     * Send message with image attachment
     */
    private fun sendMessageWithImage(message: String, imageUri: Uri) {
        // Prevent sending if already sending
        if (_uiState.value.isSending) return
        
        _uiState.update { it.copy(
            isSending = true,
            error = null
        )}

        viewModelScope.launch {
            try {
                sendMessageWithImageUseCase(message, imageUri, _uiState.value.conversationId).collect { chatMessage ->
                    _uiState.update { state ->
                        val existingMessages = state.messages.toMutableList()
                        
                        // Check if this message already exists (for streaming updates)
                        val existingIndex = existingMessages.indexOfFirst { it.id == chatMessage.id }
                        
                        if (existingIndex >= 0) {
                            // Update existing message
                            existingMessages[existingIndex] = chatMessage
                        } else {
                            // Add new message
                            existingMessages.add(chatMessage)
                        }
                        
                        state.copy(
                            messages = existingMessages,
                            isSending = chatMessage.isStreaming,
                            error = null
                        )
                    }

                    if (!chatMessage.isStreaming) {
                        _events.emit(ChatEvent.ScrollToBottom)
                    }
                }
                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSending = false,
                    error = e.message ?: "Failed to analyze image"
                )}
                _events.emit(ChatEvent.ShowError(e.message ?: "Failed to send message"))
            }
        }
    }

    /**
     * Clear all chat messages
     */
    private fun clearChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                messages = emptyList(),
                error = null
            )}
            _events.emit(ChatEvent.ShowMessage("Chat cleared"))
        }
    }

    /**
     * Retry last failed message
     */
    private fun retryLastMessage() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.isUser }
        lastUserMessage?.let {
            sendMessage(it.content)
        }
    }
    
    /**
     * Start a new chat conversation
     */
    private fun startNewChat() {
        val newConversationId = "chat_${System.currentTimeMillis()}"
        _uiState.update { it.copy(
            messages = emptyList(),
            conversationId = newConversationId,
            conversationTitle = "New Conversation",
            error = null
        )}
    }
    
    /**
     * Load existing conversation
     */
    private fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                conversationId = conversationId
            )}
            
            getChatHistoryUseCase(conversationId).collect { messages ->
                _uiState.update { it.copy(
                    messages = messages,
                    isLoading = false,
                    conversationTitle = if (messages.isNotEmpty()) 
                        messages.first().content.take(30) + "..." 
                    else "New Conversation"
                )}
            }
        }
    }

    private fun loadAllConversations() {
        viewModelScope.launch {
            try {
                getAllConversationsUseCase().collect { conversations ->
                    // Convert Conversation to ConversationItem
                    val conversationItems = conversations.map { conv ->
                        ConversationItem(
                            id = conv.id,
                            title = conv.title,
                            lastMessage = conv.lastMessage,
                            timestamp = conv.lastMessageTime,
                            messageCount = conv.messageCount
                        )
                    }
                    
                    _uiState.update { it.copy(conversations = conversationItems) }
                }
            } catch (e: Exception) {
                _events.emit(ChatEvent.ShowError(e.message ?: "Failed to load conversations"))
            }
        }
    }
}

/**
 * UI State for Chat screen
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val conversationId: String = "default",
    val conversationTitle: String = "New Conversation",
    val conversations: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

/**
 * User actions in Chat screen
 */
sealed interface ChatAction {
    data class SendMessage(val message: String) : ChatAction
    data class SendMessageWithImage(val message: String, val imageUri: Uri) : ChatAction
    data object ClearChat : ChatAction
    data object RetryLastMessage : ChatAction
    data object StartNewChat : ChatAction
    data class LoadConversation(val conversationId: String) : ChatAction
    data object LoadAllConversations : ChatAction
}

/**
 * One-time events from ViewModel
 */
sealed interface ChatEvent {
    data object ScrollToBottom : ChatEvent
    data class ShowError(val message: String) : ChatEvent
    data class ShowMessage(val message: String) : ChatEvent
}

/**
 * Data class for conversation list item
 */
data class ConversationItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long,
    val messageCount: Int
)
