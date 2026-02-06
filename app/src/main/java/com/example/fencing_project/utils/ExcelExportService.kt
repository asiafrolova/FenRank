package com.example.fencing_project.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
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

    suspend fun exportToExcel(
        opponents: List<LocalOpponent>,
        bouts: List<LocalBout>,
        fileName: String = generateFileName()
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val headerStyle = createHeaderStyle(workbook)
                val dateStyle = createDateStyle(workbook)
                createOpponentsSheet(workbook, opponents, headerStyle)
                createBoutsSheet(workbook, bouts, headerStyle, dateStyle)
                val fileUri = saveWorkbook(workbook, fileName)
                workbook.close()

                fileUri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun createOpponentsSheet(
        workbook: Workbook,
        opponents: List<LocalOpponent>,
        headerStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Соперники")


        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "ID", "Имя", "Дата создания", "Ведущая рука", "Тип оружия",
            "Комментарий", /*"Аватар URL",*/ "Создал", "Всего боев",
            "Победы пользователя", "Поражения пользователя", "Ничьи",
            "Всего нанесено уколов", "Всего получено уколов", "Дата последнего боя"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle

        }


        opponents.forEachIndexed { rowIndex, opponent ->
            val row = sheet.createRow(rowIndex + 1)

            row.createCell(0).setCellValue((opponent.id).toDouble())
            row.createCell(1).setCellValue(opponent.name)
            row.createCell(2).setCellValue(formatDate(opponent.createdAt))
            row.createCell(3).setCellValue(opponent.weaponHand)
            row.createCell(4).setCellValue(opponent.weaponType)
            row.createCell(5).setCellValue(opponent.comment ?: "")
            row.createCell(7-1).setCellValue(opponent.createdBy)
            row.createCell(8-1).setCellValue(opponent.totalBouts.toDouble())
            row.createCell(9-1).setCellValue(opponent.userWins.toDouble())
            row.createCell(10-1).setCellValue(opponent.opponentWins.toDouble())
            row.createCell(11-1).setCellValue(opponent.draws.toDouble())
            row.createCell(12-1).setCellValue(opponent.totalUserScore.toDouble())
            row.createCell(13-1).setCellValue(opponent.totalOpponentScore.toDouble())
            row.createCell(14-1).setCellValue(formatDate(opponent.lastBoutDate))
        }


    }

    private fun createBoutsSheet(
        workbook: Workbook,
        bouts: List<LocalBout>,
        headerStyle: CellStyle,
        dateStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Бои")

        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "ID", "ID соперника", "ID автора", "Уколы пользователя",
            "Уколы соперника", "Дата", "Комментарий", "Результат"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        bouts.forEachIndexed { rowIndex, bout ->
            val row = sheet.createRow(rowIndex + 1)

            row.createCell(0).setCellValue((bout.id).toDouble())
            row.createCell(1).setCellValue((bout.opponentId).toDouble())
            row.createCell(2).setCellValue(bout.authorId)
            row.createCell(3).setCellValue(bout.userScore.toDouble())
            row.createCell(4).setCellValue(bout.opponentScore.toDouble())
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

    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = true
        font.fontHeightInPoints = 12
        style.setFont(font)
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        return style
    }

    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.createDataFormat()
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
            saveToMediaStore(workbook, fileName)
        } else {
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
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
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