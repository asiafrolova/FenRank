package com.example.fencing_project.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String {
    val pattern = "dd.MM.yyyy HH:mm"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}

fun formatDate(timestamp: Long): String {
    return formatDate(Date(timestamp))
}