// ExcelExportService.kt
package com.example.fencing_project.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {


    // ExcelExportService.kt
    suspend fun exportToExcel(
        opponents: List<com.example.fencing_project.data.model.Opponent>,
        bouts: List<com.example.fencing_project.data.model.Bout>,
        fileName: String = generateFileName()
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Создаем новую книгу Excel
                val workbook = XSSFWorkbook()

                // Создаем стили
                val headerStyle = createHeaderStyle(workbook)
                val dateStyle = createDateStyle(workbook)

                // 1. Лист "Соперники"
                createOpponentsSheet(workbook, opponents, headerStyle)

                // 2. Лист "Бои"
                createBoutsSheet(workbook, bouts, headerStyle, dateStyle)

                // Сохраняем файл
                val fileUri = saveWorkbook(workbook, fileName)

                // Закрываем книгу
                workbook.close()

                fileUri // возвращаем строку URI
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun createOpponentsSheet(
        workbook: Workbook,
        opponents: List<com.example.fencing_project.data.model.Opponent>,
        headerStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Соперники")

        // Создаем заголовки
        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "ID", "Имя", "Дата создания", "Ведущая рука", "Тип оружия",
            "Комментарий", "Аватар URL", "Создал", "Всего боев",
            "Победы пользователя", "Поражения пользователя", "Ничьи",
            "Всего нанесено уколов", "Всего получено уколов", "Дата последнего боя"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle

        }

        // Заполняем данными
        opponents.forEachIndexed { rowIndex, opponent ->
            val row = sheet.createRow(rowIndex + 1)

            row.createCell(0).setCellValue(opponent.id)
            row.createCell(1).setCellValue(opponent.name)
            row.createCell(2).setCellValue(formatDate(opponent.createdAt))
            row.createCell(3).setCellValue(opponent.weaponHand)
            row.createCell(4).setCellValue(opponent.weaponType)
            row.createCell(5).setCellValue(opponent.comment ?: "")
            row.createCell(6).setCellValue(opponent.avatarUrl ?: "")
            row.createCell(7).setCellValue(opponent.createdBy)
            row.createCell(8).setCellValue(opponent.totalBouts.toDouble())
            row.createCell(9).setCellValue(opponent.userWins.toDouble())
            row.createCell(10).setCellValue(opponent.opponentWins.toDouble())
            row.createCell(11).setCellValue(opponent.draws.toDouble())
            row.createCell(12).setCellValue(opponent.totalUserScore.toDouble())
            row.createCell(13).setCellValue(opponent.totalOpponentScore.toDouble())
            row.createCell(14).setCellValue(formatDate(opponent.lastBoutDate))
        }

        // Авторазмер колонок
//        for (i in 0 until headers.size) {
//            sheet.autoSizeColumn(i)
//        }
    }

    private fun createBoutsSheet(
        workbook: Workbook,
        bouts: List<com.example.fencing_project.data.model.Bout>,
        headerStyle: CellStyle,
        dateStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Бои")

        // Создаем заголовки
        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "ID", "ID соперника", "ID автора", "Уколы пользователя",
            "Уколы соперника", "Дата", "Комментарий", "Результат"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
            //sheet.autoSizeColumn(index)
        }

        // Заполняем данными
        bouts.forEachIndexed { rowIndex, bout ->
            val row = sheet.createRow(rowIndex + 1)

            row.createCell(0).setCellValue(bout.id)
            row.createCell(1).setCellValue(bout.opponentId)
            row.createCell(2).setCellValue(bout.authorId)
            row.createCell(3).setCellValue(bout.userScore.toDouble())
            row.createCell(4).setCellValue(bout.opponentScore.toDouble())

            // Ячейка с датой
            val dateCell = row.createCell(5)
            dateCell.setCellValue(Date(bout.date))
            dateCell.cellStyle = dateStyle

            row.createCell(6).setCellValue(bout.comment ?: "")
            row.createCell(7).setCellValue(
                when {
                    bout.userScore > bout.opponentScore -> "Победа"
                    bout.userScore < bout.opponentScore -> "Поражение"
                    else -> "Ничья"
                }
            )
        }

        // Авторазмер колонок
        for (i in 0 until headers.size) {
            //sheet.autoSizeColumn(i)
        }
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = true
        font.fontHeightInPoints = 12
        style.setFont(font)

        // Границы
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN

        // Заливка
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND

        // Выравнивание
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        return style
    }

    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.createDataFormat()

            //style.dataFormat = format.getFormat("dd.MM.yyyy HH:mm")
        style.dataFormat = format.getFormat("dd.MM.yyyy")

        return style
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp != null && timestamp > 0) {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else ""
    }

    private fun generateFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "fencing_data_${sdf.format(Date())}.xlsx"
    }

    private fun saveWorkbook(workbook: Workbook, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ используем MediaStore
            saveToMediaStore(workbook, fileName)
        } else {
            // Для старых версий - во внешнее хранилище
            saveToExternalStorage(workbook, fileName)
        }
    }

    private fun saveToMediaStore(workbook: Workbook, fileName: String): String {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            // Android 9 и ниже
            resolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            )
        } ?: throw Exception("Не удалось создать файл")

        resolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        }

        return uri.toString()
    }

    private fun saveToExternalStorage(workbook: Workbook, fileName: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        ).toString()
    }
}