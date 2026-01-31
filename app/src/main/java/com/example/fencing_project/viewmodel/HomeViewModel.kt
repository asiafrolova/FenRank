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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BoutRepository,
    private val authRepository: AuthRepository,
    private val storageManager: SupabaseStorageManager
) : ViewModel() {

    private val _bouts = MutableStateFlow<UIState<List<Bout>>>(UIState.Loading)
    val bouts = _bouts.asStateFlow()

    private val _opponents = MutableStateFlow<UIState<List<Opponent>>>(UIState.Loading)
    val opponents = _opponents.asStateFlow()

    private val _addOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val addOpponentState = _addOpponentState.asStateFlow()

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                val (boutsList, opponentsList) = repository.getHomeScreenData(userId)
                _bouts.value = UIState.Success(boutsList)
                _opponents.value = UIState.Success(opponentsList)
            } catch (e: Exception) {
                _bouts.value = UIState.Error(e.message ?: "Unknown error")
                _opponents.value = UIState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun addOpponent(createdBy: String, name: String, weaponHand: String,  weaponType: String, comment:String = "", avatarUri: Uri? = null) {
        _addOpponentState.value = UIState.Loading
        viewModelScope.launch {
            try {
                println("DEBUG: Начинаем добавление соперника")

                val opponent = Opponent(
                    id = "", // Будет создан в репозитории ,
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    weaponHand =weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    avatarUrl = "",
                    createdBy = createdBy
                )

                println("DEBUG: Opponent object created: $opponent")

                val opponentId = repository.addOpponent(opponent)

                println("DEBUG: Соперник добавлен с ID: $opponentId")
                var avatarUrl = ""

                // Если есть аватарка, загружаем ее
                if (avatarUri != null && opponentId.isNotBlank()) {
                    println("DEBUG: Загружаем аватарку...")
                    val uploadedUrl = storageManager.uploadOpponentAvatar(
                        userId = createdBy,
                        opponentId = opponentId,
                        imageUri = avatarUri
                    )

                    avatarUrl = uploadedUrl ?: ""
                    if (avatarUrl.isNotBlank()) {
                        println("DEBUG: Обновляем соперника с URL аватарки: $avatarUrl")
                        repository.updateOpponentAvatar(opponentId, avatarUrl)
                    }
                }

                _addOpponentState.value = UIState.Success("Соперник добавлен")

                // Обновляем список
                loadUserData(createdBy)

            } catch (e: Exception) {
                println("DEBUG: Ошибка добавления: ${e.message}")
                e.printStackTrace()
                _addOpponentState.value = UIState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }
    fun resetAddOpponentState() {
        _addOpponentState.value = UIState.Idle
    }

    fun getCurrentUser() = authRepository.getCurrentUser()
}