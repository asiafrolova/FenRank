// ExcelImportService.kt - упрощенная версия
package com.example.fencing_project.utils

import android.content.Context
import android.net.Uri
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// ExcelImportService.kt - ТОЛЬКО парсинг, без логики сохранения
@Singleton
class ExcelImportService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class ImportedData(
        val opponents: List<Opponent> = emptyList(),
        val bouts: List<Bout> = emptyList(),
        val error: String? = null
    )

    suspend fun parseExcelFile(uri: Uri): ImportedData {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseWorkbook(inputStream)
                } ?: ImportedData(error = "Не удалось открыть файл")
            } catch (e: Exception) {
                ImportedData(error = "Ошибка парсинга: ${e.message}")
            }
        }
    }

    private fun parseWorkbook(inputStream: InputStream): ImportedData {
        val workbook = XSSFWorkbook(inputStream)

        return try {
            // 1. Парсим соперников
            val opponentsSheet = workbook.getSheet("Соперники")
            val opponents = opponentsSheet?.let { parseOpponents(it) } ?: emptyList()

            // 2. Парсим бои
            val boutsSheet = workbook.getSheet("Бои")
            val bouts = boutsSheet?.let { parseBouts(it) } ?: emptyList()

            ImportedData(opponents = opponents, bouts = bouts)

        } finally {
            workbook.close()
        }
    }

    private fun parseOpponents(sheet: Sheet): List<Opponent> {
        val opponents = mutableListOf<Opponent>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            if(isRowEmpty(row)){
                continue
            }

            try {
                val createdAtString = getCellValue(row.getCell(2))
                val createdAt = parseDate(createdAtString, dateFormat)

                // Парсим дату последнего боя из колонки 14
                val lastBoutDateString = getCellValue(row.getCell(14))
                val lastBoutDate = if (lastBoutDateString.isNotEmpty()) {
                    parseDate(lastBoutDateString, dateFormat)
                } else {
                    null
                }
                val opponent = Opponent(
                    id = getCellValue(row.getCell(0)), // Оригинальный ID из файла
                    name = getCellValue(row.getCell(1)),
                    weaponHand = getCellValue(row.getCell(3)),
                    weaponType = getCellValue(row.getCell(4)),
                    comment = getCellValue(row.getCell(5)),
                    avatarUrl = getCellValue(row.getCell(6)),
                    createdBy = getCellValue(row.getCell(7)),
                    createdAt =  createdAt,
                    //totalBouts = getCellIntValue(row.getCell(8)),
                    //userWins = getCellIntValue(row.getCell(9)),
                    //opponentWins = getCellIntValue(row.getCell(10)),
                    //draws = getCellIntValue(row.getCell(11)),
                    //totalUserScore = getCellIntValue(row.getCell(12)),
                    //totalOpponentScore = getCellIntValue(row.getCell(13)),
                    //lastBoutDate = lastBoutDate
                )

                opponents.add(opponent)

            } catch (e: Exception) {
                // Пропускаем ошибки
            }
        }

        return opponents
    }

    private fun parseBouts(sheet: Sheet): List<Bout> {
        val bouts = mutableListOf<Bout>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if(isRowEmpty(row)){
                continue
            }

            try {
                val dateString = getCellValue(row.getCell(5))
                val date = parseDate(dateString, dateFormat)
                val bout = Bout(
                    id = getCellValue(row.getCell(0)), // Оригинальный ID боя
                    opponentId = getCellValue(row.getCell(1)), // Оригинальный ID соперника
                    authorId = getCellValue(row.getCell(2)),
                    userScore = getCellIntValue(row.getCell(3)),
                    opponentScore = getCellIntValue(row.getCell(4)),
                    date = date,
                    comment = getCellValue(row.getCell(6))
                )

                bouts.add(bout)

            } catch (e: Exception) {
                // Пропускаем ошибки
            }
        }

        return bouts
    }
    private fun isRowEmpty(row: Row): Boolean {
        // Проверяем первые несколько колонок на пустоту
        for (i in 0..2) {
            val cell = row.getCell(i)
            val value = getCellValue(cell)
            if (value.isNotBlank()) {
                return false
            }
        }
        return true
    }

    private fun parseDate(excelSerialNumber: String, dateFormat: SimpleDateFormat? = null): Long {
        return try {
            // Пытаемся преобразовать строку в число (Excel серийный номер)
            val serialNumber = excelSerialNumber.toDoubleOrNull()

            if (serialNumber != null) {
                // Это Excel серийный номер - конвертируем в дату
                convertExcelSerialNumberToDate(serialNumber)
            } else {
                // Это строка в формате dd.MM.yyyy HH:mm
                dateFormat?.parse(excelSerialNumber)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun convertExcelSerialNumberToDate(serialNumber: Double): Long {
        // Excel считает дни от 30 декабря 1899 года
        // Но на самом деле Excel имеет баг - считает 1900 год високосным
        // Поэтому используем стандартный алгоритм конвертации

        val baseDate = Calendar.getInstance().apply {
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = serialNumber.toLong()
        val fraction = serialNumber - days

        // Добавляем дни
        baseDate.add(Calendar.DAY_OF_MONTH, days.toInt())

        // Добавляем дробную часть дня (время)
        val millisecondsInDay = 24 * 60 * 60 * 1000
        val timeMillis = (fraction * millisecondsInDay).toLong()

        return baseDate.time.time + timeMillis
    }



    private fun getCellValue(cell: Cell?): String {
        return when {
            cell == null -> ""
            cell.cellType == CellType.STRING -> cell.stringCellValue.trim()
            cell.cellType == CellType.NUMERIC -> cell.numericCellValue.toInt().toString()
            else -> ""
        }
    }

    private fun getCellIntValue(cell: Cell?): Int {
        return try {
            when {
                cell == null -> 0
                cell.cellType == CellType.NUMERIC -> cell.numericCellValue.toInt()
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}