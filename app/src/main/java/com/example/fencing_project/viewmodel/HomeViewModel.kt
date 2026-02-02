package com.example.fencing_project.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.SharedPrefsManager
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
    //private val repository: BoutRepository,
    private val repository: LocalBoutRepository,
    private val authRepository: AuthRepository,
    private val storageManager: SupabaseStorageManager,

) : ViewModel() {

//    private val _bouts = MutableStateFlow<UIState<List<Bout>>>(UIState.Loading)
//    val bouts = _bouts.asStateFlow()
//
//    private val _opponents = MutableStateFlow<UIState<List<Opponent>>>(UIState.Loading)
//    val opponents = _opponents.asStateFlow()
    private val _bouts = MutableStateFlow<UIState<List<LocalBout>>>(UIState.Idle)
    val bouts: StateFlow<UIState<List<LocalBout>>> = _bouts.asStateFlow()

    private val _opponents = MutableStateFlow<UIState<List<LocalOpponent>>>(UIState.Idle)
    val opponents: StateFlow<UIState<List<LocalOpponent>>> = _opponents.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        // Инициализируем текущего пользователя
        //_currentUserId.value = authRepository.getCurrentUser()?.uid
        _currentUserId.value = authRepository.getUserId()


        // Начинаем слушать данные если пользователь есть
        viewModelScope.launch {
            _currentUserId.collect { userId ->
                if (userId != null) {
                    loadUserData(userId)
                }
            }
        }
    }
    fun getCurrentUser() = authRepository.getCurrentUser()

    // Основной метод загрузки данных
    fun loadUserData(userId: String) {
        if (userId.isBlank()) {
            _bouts.value = UIState.Error("ID пользователя пустой")
            _opponents.value = UIState.Error("ID пользователя пустой")
            return
        }

        viewModelScope.launch {
            try {
                println("DEBUG: Начинаем загрузку локальных данных для $userId")

                // Подписываемся на Flow для боев
                repository.getBoutsByUser(userId)
                    .catch { e ->
                        println("DEBUG: Ошибка получения боев: ${e.message}")
                        _bouts.value = UIState.Error("Ошибка загрузки боев: ${e.message}")
                    }
                    .collect { boutsList ->
                        println("DEBUG: Получены бои: ${boutsList.size}")
                        _bouts.value = UIState.Success(boutsList)
                    }

            } catch (e: Exception) {
                println("DEBUG: Ошибка в loadUserData для боев: ${e.message}")
                _bouts.value = UIState.Error("Ошибка загрузки боев: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                // Подписываемся на Flow для соперников
                repository.getOpponentsByUser(userId)
                    .catch { e ->
                        println("DEBUG: Ошибка получения соперников: ${e.message}")
                        _opponents.value = UIState.Error("Ошибка загрузки соперников: ${e.message}")
                    }
                    .collect { opponentsList ->
                        println("DEBUG: Получены соперники: ${opponentsList.size}")
                        _opponents.value = UIState.Success(opponentsList)
                    }

            } catch (e: Exception) {
                println("DEBUG: Ошибка в loadUserData для соперников: ${e.message}")
                _opponents.value = UIState.Error("Ошибка загрузки соперников: ${e.message}")
            }
        }
    }

    // Метод для принудительного обновления
    fun refreshData() {
        val userId = _currentUserId.value
        if (userId != null) {
            loadUserData(userId)
        }
    }


    private val _addOpponentState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val addOpponentState = _addOpponentState.asStateFlow()

//    fun loadUserData(userId: String) {
//        viewModelScope.launch {
//            try {
//                val (boutsList, opponentsList) = repository.getHomeScreenData(userId)
//                _bouts.value = UIState.Success(boutsList)
//                _opponents.value = UIState.Success(opponentsList)
//            } catch (e: Exception) {
//                _bouts.value = UIState.Error(e.message ?: "Unknown error")
//                _opponents.value = UIState.Error(e.message ?: "Unknown error")
//            }
//        }
//    }
    fun addOpponent(createdBy: String, name: String, weaponHand: String,  weaponType: String, comment:String = "", avatarUri: Uri? = null) {
        _addOpponentState.value = UIState.Loading
        viewModelScope.launch {
            try {
                println("DEBUG: Начинаем добавление соперника")

                val opponent = LocalOpponent(
                    id = 0, // Будет создан в репозитории ,
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    weaponHand = weaponHand,
                    weaponType = weaponType,
                    comment = comment,
                    avatarPath = "",
                    createdBy = createdBy
                )

                println("DEBUG: Opponent object created: $opponent")

                val opponentId = repository.addOpponent(opponent)

                println("DEBUG: Соперник добавлен с ID: $opponentId")
                var avatarUrl = ""

                // Если есть аватарка, загружаем ее
                if (avatarUri != null && opponentId.equals(0)) {
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

    //fun getCurrentUser() = authRepository.getCurrentUser()
}