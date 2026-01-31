// OpponentViewModel.kt
package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class OpponentViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: BoutRepository,
    private val storageManager: SupabaseStorageManager
) : ViewModel() {

    private val _saveOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val saveOpponentState = _saveOpponentState.asStateFlow()

    private val _opponentState = MutableStateFlow<UIState<Opponent>>(UIState.Idle)
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
                val opponent = Opponent(
                    id = "",
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    weaponHand = weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    avatarUrl = "",
                    createdBy = createdBy
                )

                val opponentId = repository.addOpponent(opponent)
                var avatarUrl = ""

                // Если есть аватарка, загружаем ее
                if (avatarUri != null && opponentId.isNotBlank()) {
                    val uploadedUrl = storageManager.uploadOpponentAvatar(
                        userId = createdBy,
                        opponentId = opponentId,
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

    // OpponentViewModel.kt
    fun updateOpponent(
        opponentId: String,
        name: String,
        weaponHand: String,
        weaponType: String,
        comment: String,
        avatarUri: Uri?
    ) {
        _saveOpponentState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    println("DEBUG: Начинаем обновление соперника $opponentId")

                    // 1. Получаем старого соперника
                    val oldOpponent = repository.getOpponent(opponentId)
                    val oldAvatarUrl = oldOpponent?.avatarUrl
                    println("DEBUG: Старая аватарка: $oldAvatarUrl")

                    var newAvatarUrl = oldAvatarUrl

                    // 2. Обрабатываем аватарку (если есть новая)
                    if (avatarUri != null) {
                        println("DEBUG: Загружаем новую аватарку...")

                        // Сначала загружаем новую
                        val uploadedUrl = storageManager.uploadOpponentAvatar(
                            userId = currentUser.uid,
                            opponentId = opponentId,
                            imageUri = avatarUri
                        )

                        if (uploadedUrl != null) {
                            newAvatarUrl = uploadedUrl
                            println("DEBUG: Новая аватарка загружена: $newAvatarUrl")

                            // ТЕПЕРЬ удаляем старую (после успешной загрузки новой)
                            if (!oldAvatarUrl.isNullOrBlank()) {
                                println("DEBUG: Удаляем старую аватарку: $oldAvatarUrl")
                                storageManager.deleteOpponentAvatar(currentUser.uid,oldAvatarUrl)
                            }
                        } else {
                            println("DEBUG: Не удалось загрузить новую аватарку")
                            // Оставляем старую
                        }
                    }

                    // 3. Обновляем в Firebase и ЖДЕМ завершения
                    println("DEBUG: Обновляем данные в Firebase...")
                    repository.updateOpponent(
                        opponentId = opponentId,
                        name = name,
                        weaponHand = weaponHand,
                        weaponType = weaponType,
                        comment = comment,
                        avatarUrl = newAvatarUrl ?: ""
                    )

                    // 4. Даем время на синхронизацию
                    //delay(500) // Небольшая задержка для синхронизации

                    // 5. Проверяем, что обновилось
                    val updatedOpponent = repository.getOpponent(opponentId)
                    println("DEBUG: Проверяем обновленного соперника:")
                    println("DEBUG: - Имя: ${updatedOpponent?.name}")
                    println("DEBUG: - Новая аватарка: ${updatedOpponent?.avatarUrl}")

                    _saveOpponentState.value = UIState.Success("Соперник обновлен")

                } else {
                    _saveOpponentState.value = UIState.Error("Пользователь не авторизован")
                }
            } catch (e: Exception) {
                println("DEBUG: Ошибка обновления: ${e.message}")
                e.printStackTrace()
                _saveOpponentState.value = UIState.Error(e.message ?: "Ошибка обновления")
            }
        }
    }

//    fun updateOpponent(
//        opponentId: String,
//        name: String,
//        weaponHand: String,
//        weaponType: String,
//        comment: String = "",
//        avatarUri: Uri? = null
//    ) {
//        viewModelScope.launch {
//            _saveOpponentState.value = UIState.Loading
//            try {
//                val currentUser = authRepository.currentUser()
//                if (currentUser != null) {
//                    // Получаем текущего соперника
//                    val currentOpponent = repository.getOpponent(opponentId)
//                    if (currentOpponent != null) {
//                        var avatarUrl = currentOpponent.avatarUrl
//
//                        if (!avatarUrl.isNullOrBlank()) {
//                            println("DEBUG: Удаляем старую аватарку: $avatarUrl")
//                            storageManager.deleteOpponentAvatar(
//                                userId = currentUser.uid,
//                                opponentId = opponentId
//                            )
//                        }
//                        // Если есть новая аватарка, загружаем ее
//                        if (avatarUri != null) {
//                            val uploadedUrl = storageManager.uploadOpponentAvatar(
//                                userId = currentOpponent.createdBy,
//                                opponentId = opponentId,
//                                imageUri = avatarUri
//                            )
//                            avatarUrl = uploadedUrl ?: avatarUrl
//                        }
//
//                        // Обновляем соперника
//                        repository.updateOpponent(
//                            opponentId = opponentId,
//                            name = name,
//                            weaponHand = weaponHand,
//                            weaponType = weaponType,
//                            comment = comment,
//                            avatarUrl = avatarUrl
//                        )
//
//                        _saveOpponentState.value = UIState.Success("Соперник обновлен")
//                    } else {
//                        _saveOpponentState.value = UIState.Error("Соперник не найден")
//                    }
//                }
//                } catch (e: Exception) {
//                    _saveOpponentState.value = UIState.Error(e.message ?: "Ошибка обновления")
//                }
//
//        }
//    }

    fun deleteOpponent(opponentId: String) {
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

    fun getOpponent(opponentId: String) {
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

    fun deleteOpponentWithAvatar(opponentId: String, opponentAvatarUrl: String? = null) {
        _deleteOpponentState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val success = repository.deleteOpponentWithAvatar(
                        opponentId = opponentId,
                        userId = currentUser.uid,
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
    fun resetSaveState() {
        _saveOpponentState.value = UIState.Idle
    }

    fun resetDeleteState() {
        _deleteOpponentState.value = UIState.Idle
    }
}