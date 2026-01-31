// ExportViewModel.kt
package com.example.fencing_project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.ExcelExportService
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val boutRepository: BoutRepository,
    private val excelExportService: ExcelExportService
) : ViewModel() {

    private val _exportState = MutableStateFlow<UIState<String>>(UIState.Idle)
    val exportState = _exportState.asStateFlow()

    fun exportData(userId: String) {
        _exportState.value = UIState.Loading

        viewModelScope.launch {
            try {
                // Получаем данные пользователя
                val (bouts, opponents) = boutRepository.getHomeScreenData(userId)

                // Экспортируем в Excel - предположим, возвращает String?
                val result = excelExportService.exportToExcel(opponents, bouts)

                if (result != null) {
                    _exportState.value = UIState.Success(result)
                } else {
                    _exportState.value = UIState.Error("Не удалось создать файл")
                }
            } catch (e: Exception) {
                _exportState.value = UIState.Error(e.message ?: "Ошибка экспорта")
            }
        }
    }

    fun resetState() {
        _exportState.value = UIState.Idle
    }
}