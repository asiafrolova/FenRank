package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LocalBoutRepository,
    private val authRepository: AuthRepository,
    private val storageManager: SupabaseStorageManager,

) : ViewModel() {

    private val _bouts = MutableStateFlow<UIState<List<LocalBout>>>(UIState.Idle)
    val bouts: StateFlow<UIState<List<LocalBout>>> = _bouts.asStateFlow()

    private val _opponents = MutableStateFlow<UIState<List<LocalOpponent>>>(UIState.Idle)
    val opponents: StateFlow<UIState<List<LocalOpponent>>> = _opponents.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        _currentUserId.value = authRepository.getUserId()

        viewModelScope.launch {
            _currentUserId.collect { userId ->
                if (userId != null) {
                    loadUserData(userId)
                }
            }
        }
    }
    fun getCurrentUser() = authRepository.getCurrentUser()

    fun loadUserData(userId: String) {
        if (userId.isBlank()) {
            _bouts.value = UIState.Error("ID пользователя пустой")
            _opponents.value = UIState.Error("ID пользователя пустой")
            return
        }

        viewModelScope.launch {
            try {
                repository.getBoutsByUser(userId)
                    .catch { e ->
                        _bouts.value = UIState.Error("Ошибка загрузки боев: ${e.message}")
                    }
                    .collect { boutsList ->
                        _bouts.value = UIState.Success(boutsList)
                    }

            } catch (e: Exception) {
                _bouts.value = UIState.Error("Ошибка загрузки боев: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                repository.getOpponentsByUser(userId)
                    .catch { e ->
                        _opponents.value = UIState.Error("Ошибка загрузки соперников: ${e.message}")
                    }
                    .collect { opponentsList ->
                        _opponents.value = UIState.Success(opponentsList)
                    }

            } catch (e: Exception) {
                _opponents.value = UIState.Error("Ошибка загрузки соперников: ${e.message}")
            }
        }
    }

    fun refreshData() {
        val userId = _currentUserId.value
        if (userId != null) {
            loadUserData(userId)
        }
    }


    private val _addOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val addOpponentState = _addOpponentState.asStateFlow()
    fun addOpponent(createdBy: String, name: String, weaponHand: String,  weaponType: String, comment:String = "", avatarUri: Uri? = null) {
        _addOpponentState.value = UIState.Loading
        viewModelScope.launch {
            try {

                val opponent = LocalOpponent(
                    id = 0,
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    weaponHand = weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    avatarPath = "",
                    createdBy = createdBy
                )

                val opponentId = repository.addOpponent(opponent)
                var avatarUrl = ""
                if (avatarUri != null && opponentId.equals(0)) {
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

                _addOpponentState.value = UIState.Success("Соперник добавлен")
                loadUserData(createdBy)

            } catch (e: Exception) {
                e.printStackTrace()
                _addOpponentState.value = UIState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }
    fun resetAddOpponentState() {
        _addOpponentState.value = UIState.Idle
    }

}