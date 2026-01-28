package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.utils.SharedPrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//@HiltViewModel
//class LoginViewModel @Inject constructor(
//    private val repository: AuthRepository
//
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
//    val uiState: StateFlow<LoginUiState> = _uiState
//
//    fun login(email: String, password: String): String {
//        if (email.isBlank() || password.isBlank()) {
//            _uiState.value = LoginUiState.Error("Введите email и пароль")
//            return ""
//        }
//
//        _uiState.value = LoginUiState.Loading
//
//        viewModelScope.launch {
//            val result = repository.login(email, password)
//
//            _uiState.value = if (result.isSuccess) {
//                LoginUiState.Success
//
//            } else {
//                LoginUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Ошибка входа")
//            }
//        }
//        return repository.currentUser()?.uid ?:""
//
//    }
//
//    fun resetState() {
//        _uiState.value = LoginUiState.Idle
//    }
//
//
//}
//
//sealed class LoginUiState {
//    object Idle : LoginUiState()
//    object Loading : LoginUiState()
//    object Success : LoginUiState()
//    data class Error(val message: String) : LoginUiState()
//}

// LoginViewModel.kt
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    // Получаем UID пользователя из Firebase
                    val userId = user?.uid ?: ""
                    println("DEBUG: Пользователь вошел, UID: $userId")
                    _uiState.value = LoginUiState.Success(userId)
                } else {
                    _uiState.value = LoginUiState.Error(
                        result.exceptionOrNull()?.message ?: "Ошибка входа"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

// LoginUiState.kt
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: String) : LoginUiState() // Добавляем userId
    data class Error(val message: String) : LoginUiState()
}
