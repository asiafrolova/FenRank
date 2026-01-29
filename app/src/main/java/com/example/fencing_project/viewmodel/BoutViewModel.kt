package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//@HiltViewModel
//class BoutViewModel @Inject constructor(
//    private val repository: BoutRepository
//) : ViewModel() {
//
//    val addBoutState = MutableStateFlow<UIState<String>>(UIState.Idle)
//
//    fun addBout(bout: Bout) {
//        addBoutState.value = UIState.Loading
//        viewModelScope.launch {
//            try {
//                val boutId = repository.addBout(bout)
//                addBoutState.value = UIState.Success("Бой добавлен! ID: $boutId")
//            } catch (e: Exception) {
//                addBoutState.value = UIState.Error(e.message ?: "Ошибка добавления боя")
//            }
//        }
//    }
//
//    fun resetState() {
//        addBoutState.value = UIState.Idle
//    }
//}

// BoutViewModel.kt
@HiltViewModel
class BoutViewModel @Inject constructor(
    private val repository: BoutRepository
) : ViewModel() {

    private val _saveBoutState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val saveBoutState = _saveBoutState.asStateFlow()

    private val _boutState = MutableStateFlow<UIState<Bout>>(UIState.Idle)
    val boutState = _boutState.asStateFlow()

    private val _deleteBoutState = MutableStateFlow<UIState<Boolean>>(UIState.Idle)
    val deleteBoutState = _deleteBoutState.asStateFlow()

    fun addBout(bout: Bout) {
        viewModelScope.launch {
            _saveBoutState.value = UIState.Loading
            try {
                repository.addBout(bout)
                _saveBoutState.value = UIState.Success("Бой добавлен")
            } catch (e: Exception) {
                _saveBoutState.value = UIState.Error(e.message ?: "Ошибка добавления боя")
            }
        }
    }

    fun updateBout(bout: Bout) {
        viewModelScope.launch {
            _saveBoutState.value = UIState.Loading
            try {
                repository.updateBout(bout)
                _saveBoutState.value = UIState.Success("Бой обновлен")
            } catch (e: Exception) {
                _saveBoutState.value = UIState.Error(e.message ?: "Ошибка обновления боя")
            }
        }
    }

    fun deleteBout(boutId: String) {
        viewModelScope.launch {
            _deleteBoutState.value = UIState.Loading
            try {
                repository.deleteBout(boutId)
                _deleteBoutState.value = UIState.Success(true)
            } catch (e: Exception) {
                _deleteBoutState.value = UIState.Error(e.message ?: "Ошибка удаления боя")
            }
        }
    }

    fun getBout(boutId: String) {
        viewModelScope.launch {
            _boutState.value = UIState.Loading
            try {
                val bout = repository.getBout(boutId)
                _boutState.value = if (bout != null) {
                    UIState.Success(bout)
                } else {
                    UIState.Error("Бой не найден")
                }
            } catch (e: Exception) {
                _boutState.value = UIState.Error(e.message ?: "Ошибка загрузки боя")
            }
        }
    }

    fun resetSaveState() {
        _saveBoutState.value = UIState.Idle
    }

    fun resetDeleteState() {
        _deleteBoutState.value = UIState.Idle
    }

    fun resetAllStates() {
        _saveBoutState.value = UIState.Idle
        _boutState.value = UIState.Idle
        _deleteBoutState.value = UIState.Idle
    }
}