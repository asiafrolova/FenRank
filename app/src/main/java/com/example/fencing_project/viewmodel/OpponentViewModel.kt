package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.data.local.LocalStorageManager
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OpponentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: LocalBoutRepository,
    private val storageManager: LocalStorageManager
) : ViewModel() {

    private val _saveOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val saveOpponentState = _saveOpponentState.asStateFlow()

    private val _opponentState = MutableStateFlow<UIState<LocalOpponent>>(UIState.Idle)
    val opponentState = _opponentState.asStateFlow()

    private val _deleteOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val deleteOpponentState = _deleteOpponentState.asStateFlow()

    fun addOpponent(
        createdBy: String,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String = "",
        avatarUri: Uri? = null
    ) {
        viewModelScope.launch {
            _saveOpponentState.value = UIState.Loading
            try {
                val opponent = LocalOpponent(
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    weaponHand = weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    createdBy = createdBy
                )

                val opponentId = repository.addOpponent(opponent)
                var avatarUrl = ""
                if (avatarUri != null ) {
                    val uploadedUrl = storageManager.uploadOpponentAvatar(
                        userId = createdBy,
                        opponentId = (opponentId),
                        imageUri = avatarUri
                    )
                    avatarUrl = uploadedUrl ?: ""
                    if (avatarUrl.isNotBlank()) {
                        repository.updateOpponentAvatar(opponentId, avatarUrl)
                    }
                }

                _saveOpponentState.value = UIState.Success("Соперник добавлен")
            } catch (e: Exception) {
                _saveOpponentState.value = UIState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }
    fun updateOpponent(
        opponentId: Long,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String,
        avatarUri: Uri?
    ) {
        _saveOpponentState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()
                if (userId != null) {
                    val oldOpponent = repository.getOpponent(opponentId)
                    val oldAvatarUrl = oldOpponent?.avatarPath
                    var newAvatarUrl = oldAvatarUrl ?: ""

                    if (avatarUri != null) {
                        val uploadedUrl = storageManager.uploadOpponentAvatar(
                            userId = userId,
                            opponentId = opponentId,
                            imageUri = avatarUri
                        )

                        if (uploadedUrl != null) {
                            newAvatarUrl = uploadedUrl

                        }
                    }
                    val updatedOpponent = oldOpponent?.copy(
                        name = name,
                        weaponHand = weaponHand,
                        weaponType = weaponType,
                        comment = comment,
                        avatarPath = newAvatarUrl
                    ) ?: return@launch

                    repository.updateOpponent(updatedOpponent)
                    val finalOpponent = repository.getOpponent(opponentId)
                    _saveOpponentState.value = UIState.Success("Соперник обновлен")

                } else {
                    _saveOpponentState.value = UIState.Error("Пользователь не авторизован")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _saveOpponentState.value = UIState.Error(e.message ?: "Ошибка обновления")
            }
        }
    }


    fun deleteOpponent(opponentId: Long) {
        viewModelScope.launch {
            _deleteOpponentState.value = UIState.Loading
            try {
                repository.deleteOpponent(opponentId)
                _deleteOpponentState.value = UIState.Success("Соперник удален")
            } catch (e: Exception) {
                _deleteOpponentState.value = UIState.Error(e.message ?: "Ошибка удаления")
            }
        }
    }

    fun getOpponent(opponentId: Long) {
        viewModelScope.launch {
            _opponentState.value = UIState.Loading
            try {
                val opponent = repository.getOpponent(opponentId)
                _opponentState.value = if (opponent != null) {
                    UIState.Success(opponent)
                } else {
                    UIState.Error("Соперник не найден")
                }
            } catch (e: Exception) {
                _opponentState.value = UIState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun deleteOpponentWithAvatar(opponentId: Long, opponentAvatarUrl: String? = null) {
        _deleteOpponentState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()
                if (userId != null) {
                    val success = repository.deleteOpponentWithAvatar(
                        opponentId = opponentId,
                        userId = userId,
                        opponentAvatarUrl = opponentAvatarUrl
                    )

                    if (success) {
                        _deleteOpponentState.value = UIState.Success("Соперник удален")
                    } else {
                        _deleteOpponentState.value = UIState.Error("Ошибка удаления")
                    }
                } else {
                    _deleteOpponentState.value = UIState.Error("Пользователь не авторизован")
                }
            } catch (e: Exception) {
                _deleteOpponentState.value = UIState.Error(e.message ?: "Ошибка удаления")
            }
        }
    }
    fun deleteOpponentAvatar(opponentId: Long, opponentAvatarUrl: String? = null) {
        _saveOpponentState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()
                if (userId != null) {
                    val success = repository.deleteOpponentAvatar(
                        opponentId = opponentId,
                        userId = userId,
                        opponentAvatarUrl = opponentAvatarUrl
                    )

                    if (success) {
                        _saveOpponentState.value = UIState.Success("Аватар соперника удален")
                    } else {
                        _saveOpponentState.value = UIState.Error("Ошибка удаления")
                    }
                } else {
                    _saveOpponentState.value = UIState.Error("Пользователь не авторизован")
                }
            } catch (e: Exception) {
                _saveOpponentState.value = UIState.Error(e.message ?: "Ошибка удаления")
            }
        }
    }
    fun resetSaveState() {
        _saveOpponentState.value = UIState.Idle
    }

    fun resetDeleteState() {
        _deleteOpponentState.value = UIState.Idle
    }
}