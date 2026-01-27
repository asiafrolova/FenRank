package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoutViewModel @Inject constructor(
    private val repository: BoutRepository
) : ViewModel() {

    val addBoutState = MutableStateFlow<UIState<String>>(UIState.Idle)

    fun addBout(bout: Bout) {
        addBoutState.value = UIState.Loading
        viewModelScope.launch {
            try {
                val boutId = repository.addBout(bout)
                addBoutState.value = UIState.Success("Бой добавлен! ID: $boutId")
            } catch (e: Exception) {
                addBoutState.value = UIState.Error(e.message ?: "Ошибка добавления боя")
            }
        }
    }

    fun resetState() {
        addBoutState.value = UIState.Idle
    }
}