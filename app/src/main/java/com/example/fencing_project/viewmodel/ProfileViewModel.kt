package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ProfileViewModel.kt
// ProfileViewModel.kt
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageManager: SupabaseStorageManager
) : ViewModel() {

    private val _updateState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val updateState = _updateState.asStateFlow()

    // Сделай userAvatarUrl StateFlow чтобы он обновлялся
    private val _userAvatarUrl = MutableStateFlow<String?>(null)
    val userAvatarUrl = _userAvatarUrl.asStateFlow()

    init {
        // Инициализируем URL при создании ViewModel
        viewModelScope.launch {
            updateAvatarUrl()
        }
    }

    // Получаем текущего пользователя
    fun getCurrentUser() = authRepository.currentUser()

    // Обновляем URL аватарки
    private suspend fun updateAvatarUrl() {
        val user = authRepository.currentUser()
        _userAvatarUrl.value = user?.let {
            storageManager.generateUserAvatarUrl(it.uid)
        }
    }

    // Обновляем аватарку
    fun updateUserAvatar(avatarUri: Uri) {
        _updateState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val user = authRepository.currentUser()
                if (user != null) {
                    println("DEBUG: Обновление аватарки для ${user.uid}")

                    // 1. Загружаем новую
                    val avatarUrl = storageManager.uploadUserAvatar(
                        userId = user.uid,
                        imageUri = avatarUri
                    )

                    if (avatarUrl != null) {
                        println("DEBUG: Аватарка загружена, URL: $avatarUrl")

                        // 2. Обновляем StateFlow
                        _userAvatarUrl.value = avatarUrl

                        _updateState.value = UIState.Success("Аватарка обновлена")
                    } else {
                        _updateState.value = UIState.Error("Не удалось загрузить")
                    }
                } else {
                    _updateState.value = UIState.Error("Не авторизован")
                }
            } catch (e: Exception) {
                _updateState.value = UIState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun deleteUserAvatar() {
        _updateState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val user = authRepository.currentUser()
                if (user != null) {
                    val success = storageManager.deleteUserAvatar(user.uid)

                    if (success) {
                        // Обновляем URL на null
                        _userAvatarUrl.value = null

                        _updateState.value = UIState.Success("Аватарка удалена")
                    } else {
                        _updateState.value = UIState.Error("Не удалось удалить")
                    }
                } else {
                    _updateState.value = UIState.Error("Не авторизован")
                }
            } catch (e: Exception) {
                _updateState.value = UIState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun resetState() {
        _updateState.value = UIState.Idle
    }
}