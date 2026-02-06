package com.example.fencing_project.utils

import android.content.Context
import android.net.Uri
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
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

@Singleton
class ExcelImportService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class ImportedData(
        val opponents: List<LocalOpponent> = emptyList(),
        val bouts: List<LocalBout> = emptyList(),
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
            val opponentsSheet = workbook.getSheet("Соперники")
            val opponents = opponentsSheet?.let { parseOpponents(it) } ?: emptyList()
            val boutsSheet = workbook.getSheet("Бои")
            val bouts = boutsSheet?.let { parseBouts(it) } ?: emptyList()

            ImportedData(opponents = opponents, bouts = bouts)

        } finally {
            workbook.close()
        }
    }

    private fun parseOpponents(sheet: Sheet): List<LocalOpponent> {
        val opponents = mutableListOf<LocalOpponent>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            if(isRowEmpty(row)){
                continue
            }

            try {
                val createdAtString = getCellValue(row.getCell(2))
                val createdAt = parseDate(createdAtString, dateFormat)
                val lastBoutDateString = getCellValue(row.getCell(14))
                val lastBoutDate = if (lastBoutDateString.isNotEmpty()) {
                    parseDate(lastBoutDateString, dateFormat)
                } else {
                    null
                }
                val opponent = LocalOpponent(
                    id = getCellValue(row.getCell(0)).toLong(), // Оригинальный ID из файла
                    name = getCellValue(row.getCell(1)),
                    weaponHand = getCellValue(row.getCell(3)),
                    weaponType = getCellValue(row.getCell(4)),
                    comment = getCellValue(row.getCell(5)),
                    avatarPath = getCellValue(row.getCell(6)),
                    createdBy = getCellValue(row.getCell(7)),
                    createdAt =  createdAt,
                )

                opponents.add(opponent)

            } catch (e: Exception) {
                throw e
            }
        }

        return opponents
    }

    private fun parseBouts(sheet: Sheet): List<LocalBout> {
        val bouts = mutableListOf<LocalBout>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if(isRowEmpty(row)){
                continue
            }

            try {
                val dateString = getCellValue(row.getCell(5))
                val date = parseDate(dateString, dateFormat)
                val bout = LocalBout(
                    id = getCellValue(row.getCell(0)).toLong(), // Оригинальный ID боя
                    opponentId = getCellValue(row.getCell(1)).toLong(), // Оригинальный ID соперника
                    authorId = getCellValue(row.getCell(2)),
                    userScore = getCellIntValue(row.getCell(3)),
                    opponentScore = getCellIntValue(row.getCell(4)),
                    date = date,
                    comment = getCellValue(row.getCell(6))
                )

                bouts.add(bout)

            } catch (e: Exception) {
                throw e
            }
        }

        return bouts
    }
    private fun isRowEmpty(row: Row): Boolean {
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
            val serialNumber = excelSerialNumber.toDoubleOrNull()

            if (serialNumber != null) {
                convertExcelSerialNumberToDate(serialNumber)
            } else {
                dateFormat?.parse(excelSerialNumber)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun convertExcelSerialNumberToDate(serialNumber: Double): Long {
        val baseDate = Calendar.getInstance().apply {
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = serialNumber.toLong()
        val fraction = serialNumber - days
        baseDate.add(Calendar.DAY_OF_MONTH, days.toInt())
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