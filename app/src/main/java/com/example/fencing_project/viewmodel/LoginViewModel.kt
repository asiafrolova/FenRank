package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                    val userId = user?.uid ?: ""
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

    fun loginOffline(){
        _uiState.value = LoginUiState.Offline(true)
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userId: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Offline(val offline: Boolean) : LoginUiState()
}
