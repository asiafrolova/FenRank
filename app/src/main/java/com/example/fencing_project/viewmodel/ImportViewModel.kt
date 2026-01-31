// ImportViewModel.kt
package com.example.fencing_project.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.ExcelImportService
import com.example.fencing_project.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ImportViewModel.kt - СОХРАНЕНИЕ в Firebase
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val boutRepository: BoutRepository,
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
                println("=== НАЧАЛО ИМПОРТА ===")

                // 1. Парсим файл
                println("1. Парсим файл Excel...")
                val importedData = excelImportService.parseExcelFile(uri)

                if (importedData.error != null) {
                    println("Ошибка парсинга: ${importedData.error}")
                    _importState.value = UIState.Error(importedData.error)
                    return@launch
                }

                println("✓ Прочитано: ${importedData.opponents.size} соперников, ${importedData.bouts.size} боев")

                // 2. СОХРАНЯЕМ СОПЕРНИКОВ в Firebase
                println("2. Сохраняем соперников в Firebase...")
                val idMapping = mutableMapOf<String, String>() // Map<Старый_ID, Новый_ID>

                for (importedOpponent in importedData.opponents) {
                    try {
                        val oldId = importedOpponent.id // Старый ID из файла

                        // Создаем нового соперника для Firebase
                        val opponentForFirebase = importedOpponent.copy(
                            id = "", // Пустой ID - Firebase сгенерирует новый
                            createdBy = userId // Устанавливаем текущего пользователя
                        )

                        // СОХРАНЯЕМ в Firebase и получаем НОВЫЙ ID
                        val newId = boutRepository.addOpponent(opponentForFirebase)

                        // Сохраняем маппинг
                        idMapping[oldId] = newId

                        println("  Соперник '${importedOpponent.name}': $oldId -> $newId")

                    } catch (e: Exception) {
                        println("  ✗ Ошибка соперника '${importedOpponent.name}': ${e.message}")
                    }
                }

                println("✓ Сохранено соперников: ${idMapping.size}")

                // 3. СОХРАНЯЕМ БОИ в Firebase
                println("3. Сохраняем бои в Firebase...")
                var savedBouts = 0

                for (importedBout in importedData.bouts) {
                    try {
                        val oldOpponentId = importedBout.opponentId // Старый ID соперника

                        // Находим новый ID из маппинга
                        val newOpponentId = idMapping[oldOpponentId]

                        if (newOpponentId != null) {
                            // Создаем новый бой с обновленным ID соперника
                            val boutForFirebase = importedBout.copy(
                                id = "", // Пустой ID - Firebase сгенерирует новый
                                opponentId = newOpponentId, // Используем НОВЫЙ ID
                                authorId = userId // Устанавливаем текущего пользователя
                            )

                            // СОХРАНЯЕМ в Firebase
                            boutRepository.addBout(boutForFirebase)
                            savedBouts++

                            println("  Бой сохранен: старый ID соперника $oldOpponentId -> новый $newOpponentId")
                        } else {
                            println("  ✗ Пропущен бой: нет маппинга для ID соперника $oldOpponentId")
                        }

                    } catch (e: Exception) {
                        println("  ✗ Ошибка боя: ${e.message}")
                    }
                }

                println("✓ Сохранено боев: $savedBouts")
                println("=== ИМПОРТ ЗАВЕРШЕН ===")

                // 4. Возвращаем результат
                _importState.value = UIState.Success(
                    ImportResult(
                        importedOpponents = idMapping.size,
                        importedBouts = savedBouts,
                        message = "Импортировано: ${idMapping.size} соперников, $savedBouts боев"
                    )
                )

            } catch (e: Exception) {
                println("=== КРИТИЧЕСКАЯ ОШИБКА ===")
                println(e.message)
                e.printStackTrace()

                _importState.value = UIState.Error("Ошибка импорта: ${e.message}")
            }
        }
    }

    fun resetState() {
        _importState.value = UIState.Idle
    }
}