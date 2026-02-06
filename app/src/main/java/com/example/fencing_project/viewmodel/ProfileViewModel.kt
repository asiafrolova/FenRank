package com.example.fencing_project.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalStorageManager
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.AppLocaleManager
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseStorageManager: SupabaseStorageManager,
    private val firebaseBoutRepository: BoutRepository,
    private val storageManager: LocalStorageManager,
    private val boutRepository: LocalBoutRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _updateState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val updateState = _updateState.asStateFlow()

    private val _userAvatarUrl = MutableStateFlow<String?>(null)
    val userAvatarUrl = _userAvatarUrl.asStateFlow()


    private val _updateEmailState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updateEmailState = _updateEmailState.asStateFlow()

    private val _updatePasswordState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updatePasswordState = _updatePasswordState.asStateFlow()

    private val _updatePasswordStateSimple = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updatePasswordStateSimple = _updatePasswordStateSimple.asStateFlow()

    private val _deleteProfileState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val deleteProfileState = _deleteProfileState.asStateFlow()

    init {
        viewModelScope.launch {
            updateAvatarUrl()
        }
    }


    fun updateEmail(
        currentEmail: String,
        currentPassword: String,
        newEmail: String
    ) {
        _updateEmailState.value = UIState.Loading

        viewModelScope.launch {
            val result = authRepository.updateEmail(
                currentEmail = currentEmail,
                currentPassword = currentPassword,
                newEmail = newEmail
            )

            _updateEmailState.value = when {
                result.isSuccess -> UIState.Success(Unit)
                else -> UIState.Error(result.exceptionOrNull()?.message ?: "Ошибка")
            }

        }
    }

    fun updatePassword(
        currentEmail: String,
        currentPassword: String,
        newPassword: String
    ) {
        _updatePasswordState.value = UIState.Loading

        viewModelScope.launch {
            val result = authRepository.updatePassword(
                currentEmail = currentEmail,
                currentPassword = currentPassword,
                newPassword = newPassword
            )

            _updatePasswordState.value = when {
                result.isSuccess -> UIState.Success(Unit)
                else -> UIState.Error(result.exceptionOrNull()?.message ?: "Ошибка")
            }

        }
    }
    fun sendPasswordResetEmail(currentEmail: String){
        _updatePasswordStateSimple.value = UIState.Loading

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(
                email = currentEmail
            )
            _updatePasswordStateSimple.value = when {
                result.isSuccess -> UIState.Success(Unit)
                else -> UIState.Error(result.exceptionOrNull()?.message ?: "Ошибка")
            }
        }

    }

    fun getCurrentUser() = authRepository.getCurrentUser()

    private suspend fun updateAvatarUrl() {
        val userId = authRepository.getUserId()
        _userAvatarUrl.value = storageManager.generateUserAvatarUrl(userId?:"")
    }

    fun updateUserAvatar(avatarUri: Uri) {
        _updateState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()

                if (userId != null) {
                    val avatarUrl = storageManager.uploadUserAvatar(
                        userId = userId,
                        imageUri = avatarUri
                    )
                    if (avatarUrl != null) {

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
                val userId = authRepository.getUserId()
                if (userId != null) {
                    val success = storageManager.deleteUserAvatar(userId)

                    if (success) {
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
    fun logout(){
        authRepository.logout()
    }

    fun deleteAccount(currentPassword: String) {
        _deleteProfileState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()
                if (userId == null) {
                    _deleteProfileState.value = UIState.Error("Пользователь не найден")
                    return@launch
                }

                storageManager.deleteUserAvatar(userId)
                firebaseStorageManager.deleteUserAvatar(userId)
                val opponentIds = boutRepository.getOpponentsByUser(userId)
                val (bouts,firebaseOpponetnIds) = firebaseBoutRepository.getHomeScreenData(userId)
                val opponentsList = opponentIds.first()
                firebaseOpponetnIds.forEach { opponent ->
                    firebaseBoutRepository.deleteOpponent(opponent.id)
                    firebaseStorageManager.deleteOpponentAvatar(userId,(opponent.roomId).toString())
                }
                opponentsList.forEach { opponent ->
                    boutRepository.deleteOpponent(opponent.id)
                    storageManager.deleteOpponentAvatar(userId, opponent.id)
                }
                val sync = syncRepository.getSyncByUserId(userId)
                if(sync!=null){
                    syncRepository.deleteSync(sync.id)
                }

                val result = authRepository.deleteAccount(currentPassword)

                _deleteProfileState.value = if (result.isSuccess) {
                    UIState.Success(Unit)
                } else {
                    UIState.Error(result.exceptionOrNull()?.message ?: "Ошибка")
                }

            } catch (e: Exception) {
                _deleteProfileState.value = UIState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteProfileState.value = UIState.Idle
    }
}
data class SettingState(
    val selectedLanguage: String = ""
)