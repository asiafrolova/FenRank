package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _bouts = MutableStateFlow<UIState<List<Bout>>>(UIState.Loading)
    val bouts = _bouts.asStateFlow()

    private val _opponents = MutableStateFlow<UIState<List<Opponent>>>(UIState.Loading)
    val opponents = _opponents.asStateFlow()

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
    fun currentUser() = authRepository.currentUser()
}