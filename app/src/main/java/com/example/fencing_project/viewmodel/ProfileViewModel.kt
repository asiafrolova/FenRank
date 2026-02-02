package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalStorageManager

import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// ProfileViewModel.kt
// ProfileViewModel.kt
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

    // Сделай userAvatarUrl StateFlow чтобы он обновлялся
    private val _userAvatarUrl = MutableStateFlow<String?>(null)
    val userAvatarUrl = _userAvatarUrl.asStateFlow()

    init {
        // Инициализируем URL при создании ViewModel
        viewModelScope.launch {
            updateAvatarUrl()
        }
    }

    private val _updateEmailState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updateEmailState = _updateEmailState.asStateFlow()

    private val _updatePasswordState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updatePasswordState = _updatePasswordState.asStateFlow()

    private val _updatePasswordStateSimple = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val updatePasswordStateSimple = _updatePasswordStateSimple.asStateFlow()

    private val _deleteProfileState = MutableStateFlow<UIState<Unit>>(UIState.Idle)
    val deleteProfileState = _deleteProfileState.asStateFlow()



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
            println("DEBUG: state = ${_updateEmailState.value}")
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
            println("DEBUG: state = ${ _updatePasswordState.value}")
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

    // Получаем текущего пользователя
    fun getCurrentUser() = authRepository.getCurrentUser()

    // Обновляем URL аватарки
    private suspend fun updateAvatarUrl() {
//        val user = authRepository.getCurrentUser()
//        _userAvatarUrl.value = user?.let {
//            storageManager.generateUserAvatarUrl(it.uid)
//        }
        val userId = authRepository.getUserId()
        _userAvatarUrl.value = storageManager.generateUserAvatarUrl(userId?:"")
    }

    // Обновляем аватарку
    fun updateUserAvatar(avatarUri: Uri) {
        _updateState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
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
                //val user = authRepository.getCurrentUser()
                val userId = authRepository.getUserId()
                if (userId != null) {
                    val success = storageManager.deleteUserAvatar(userId)

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

    fun deleteProfile(){

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
                //val user = authRepository.getCurrentUser()
                val userId = authRepository.getUserId()
                if (userId == null) {
                    _deleteProfileState.value = UIState.Error("Пользователь не найден")
                    return@launch
                }



                // 1. Удаляем аватарку пользователя из Supabase
                storageManager.deleteUserAvatar(userId)
                firebaseStorageManager.deleteUserAvatar(userId)


                // 2. Удаляем всех соперников пользователя с их аватарками
                //val opponentIds = authRepository.getUserOpponents(userId)
                println("DEBUG: начинаем удалять соперников")
                val opponentIds = boutRepository.getOpponentsByUser(userId)
                val (bouts,firebaseOpponetnIds) = firebaseBoutRepository.getHomeScreenData(userId)
                val opponentsList = opponentIds.first()  // ← Берем только первый снимок данных
                println("DEBUG: найдено соперников для удаления: ${opponentsList.size}")
                firebaseOpponetnIds.forEach { opponent ->
                    firebaseBoutRepository.deleteOpponent(opponent.id)
                    firebaseStorageManager.deleteOpponentAvatar(userId,(opponent.roomId).toString())
                }
                opponentsList.forEach { opponent ->
                    println("DEBUG: удаляем соперника ${opponent.id} - ${opponent.name}")
                    boutRepository.deleteOpponent(opponent.id)
                    storageManager.deleteOpponentAvatar(userId, opponent.id)
                }
                val sync = syncRepository.getSyncByUserId(userId)
                if(sync!=null){
                    syncRepository.deleteSync(sync.id)
                }

                println("DEBUG: закончили удалять соперников")

                // 3. Удаляем аккаунт из Firebase Auth (удалит все данные через правила безопасности)
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

    // Добавь в конце класса сброс состояния
    fun resetDeleteAccountState() {
        _deleteProfileState.value = UIState.Idle
    }
}