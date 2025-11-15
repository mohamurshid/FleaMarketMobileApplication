package com.labs.fleamarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labs.fleamarketapp.api.ApiClient
import com.labs.fleamarketapp.api.models.NotificationDto
import com.labs.fleamarketapp.data.UiState
import kotlinx.coroutines.launch

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String,
    val isRead: Boolean = false,
    val itemId: String? = null
)

class NotificationViewModel : ViewModel() {
    
    private val api = ApiClient.api
    private var lastLoadedUserId: String? = null
    private var currentFilterUnreadOnly: Boolean = false
    
    private val _notificationsState = MutableLiveData<UiState<List<Notification>>>()
    val notificationsState: LiveData<UiState<List<Notification>>> = _notificationsState
    
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount
    
    fun loadNotifications(userId: String, unreadOnly: Boolean = false) {
        viewModelScope.launch {
            _notificationsState.value = UiState.Loading
            try {
                val response = api.getNotifications(userId, unreadOnly)
                if (response.isSuccessful && response.body()?.success == true) {
                    val notifications = response.body()?.data.orEmpty().map { it.toNotification() }
                    _notificationsState.value = UiState.Success(notifications)
                    updateUnreadCount(notifications)
                    lastLoadedUserId = userId
                    currentFilterUnreadOnly = unreadOnly
                } else {
                    _notificationsState.value = UiState.Error(response.body()?.message ?: "Failed to load notifications")
                }
            } catch (e: Exception) {
                _notificationsState.value = UiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }
    
    fun refresh() {
        val userId = lastLoadedUserId ?: return
        loadNotifications(userId, currentFilterUnreadOnly)
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch { updateNotificationReadState(notificationId, true) }
    }
    
    fun markAsUnread(notificationId: String) {
        viewModelScope.launch { updateNotificationReadState(notificationId, false) }
    }
    
    fun markAllAsRead() {
        val current = (_notificationsState.value as? UiState.Success) ?: return
        val unread = current.data.filter { !it.isRead }
        if (unread.isEmpty()) return
        
        val updated = current.data.map { it.copy(isRead = true) }
        _notificationsState.value = UiState.Success(updated)
        updateUnreadCount(updated)
        
        viewModelScope.launch {
            unread.forEach { notification ->
                try {
                    api.markNotificationAsRead(notification.id)
                } catch (_: Exception) {
                    // ignore individual failures
                }
            }
        }
    }
    
    private fun updateUnreadCount(notifications: List<Notification>) {
        _unreadCount.value = notifications.count { !it.isRead }
    }
    
    private suspend fun updateNotificationReadState(notificationId: String, isRead: Boolean) {
        try {
            val response = if (isRead) {
                api.markNotificationAsRead(notificationId)
            } else {
                api.markNotificationAsUnread(notificationId)
            }
            
            if (response.isSuccessful && response.body()?.success == true) {
                val current = _notificationsState.value
                if (current is UiState.Success) {
                    val updated = current.data.map {
                        if (it.id == notificationId) it.copy(isRead = isRead) else it
                    }
                    _notificationsState.value = UiState.Success(updated)
                    updateUnreadCount(updated)
                } else {
                    lastLoadedUserId?.let { loadNotifications(it, currentFilterUnreadOnly) }
                }
            }
        } catch (_: Exception) {
            // keep UI state unchanged
        }
    }
    
    private fun NotificationDto.toNotification(): Notification = Notification(
        id = id,
        title = title,
        message = message,
        timestamp = timestamp,
        type = type,
        isRead = isRead,
        itemId = itemId
    )
}

