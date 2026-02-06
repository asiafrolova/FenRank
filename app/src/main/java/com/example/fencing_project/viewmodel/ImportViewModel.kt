package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.utils.ExcelImportService
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val boutRepository: LocalBoutRepository,
    private val excelImportService: ExcelImportService
) : ViewModel() {

    private val _importState = MutableStateFlow<UIState<ImportResult>>(UIState.Idle)
    val importState = _importState.asStateFlow()

    data class ImportResult(
        val importedOpponents: Int = 0,
        val importedBouts: Int = 0,
        val message: String = ""
    )

    fun importData(uri: Uri, userId: String) {
        _importState.value = UIState.Loading

        viewModelScope.launch {
            try {
                val importedData = excelImportService.parseExcelFile(uri)

                if (importedData.error != null) {
                    _importState.value = UIState.Error(importedData.error)
                    return@launch
                }
                val idMapping = mutableMapOf<Long, Long>()

                for (importedOpponent in importedData.opponents) {
                    try {
                        val oldId = importedOpponent.id
                        val opponentForFirebase = importedOpponent.copy(
                            id = 0,
                            createdBy = userId
                        )
                        val newId = boutRepository.addOpponent(opponentForFirebase)
                        idMapping[oldId] = newId

                    } catch (e: Exception) {
                        throw e
                    }
                }

                var savedBouts = 0

                for (importedBout in importedData.bouts) {
                    try {
                        val oldOpponentId = importedBout.opponentId

                        val newOpponentId = idMapping[oldOpponentId]

                        if (newOpponentId != null) {
                            val boutForFirebase = importedBout.copy(
                                id = 0,
                                opponentId = newOpponentId,
                                authorId = userId
                            )
                            boutRepository.addBout(boutForFirebase)
                            savedBouts++

                        }

                    } catch (e: Exception) {
                        throw e
                    }
                }

                _importState.value = UIState.Success(
                    ImportResult(
                        importedOpponents = idMapping.size,
                        importedBouts = savedBouts,
                        message = "Импортировано: ${idMapping.size} соперников, $savedBouts боев"
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _importState.value = UIState.Error("Ошибка импорта: ${e.message}")
            }
        }
    }

    fun resetState() {
        _importState.value = UIState.Idle
    }
}