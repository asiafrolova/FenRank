package com.example.fencing_project.view

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.work.AlarmSyncScheduler // Измените импорт
import com.example.fencing_project.viewmodel.SyncViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(navController: NavController, pref: SharedPrefsManager,) {
    val viewModel: SyncViewModel = hiltViewModel()
    val context = LocalContext.current

    // Состояния
    var selectedFrequency by remember {
        mutableStateOf(AlarmSyncScheduler.Frequency.DISABLED) // Используйте AlarmSyncScheduler.Frequency
    }
    var selectedHour by remember { mutableStateOf(2) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Загружаем текущие настройки при запуске
    LaunchedEffect(Unit) {
        val schedule = viewModel.getCurrentSchedule()
        if (schedule != null) {
            selectedFrequency = schedule.frequency
            selectedHour = schedule.hour
            selectedMinute = schedule.minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расписание синхронизации", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "back",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(25, 25, 33)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(25, 25, 33))
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(44, 44, 51)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Периодичность",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // ИСПРАВЛЕННЫЙ КОД: Используем AlarmSyncScheduler.Frequency
                            AlarmSyncScheduler.Frequency.values().forEach { frequency ->
                                if (frequency != AlarmSyncScheduler.Frequency.DISABLED) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedFrequency = frequency
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedFrequency == frequency,
                                            onClick = { selectedFrequency = frequency },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(139, 0, 0),
                                                unselectedColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = frequency.displayName, // Используем displayName
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                            // Опция "Отключено"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFrequency = AlarmSyncScheduler.Frequency.DISABLED
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFrequency == AlarmSyncScheduler.Frequency.DISABLED,
                                    onClick = { selectedFrequency = AlarmSyncScheduler.Frequency.DISABLED },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(139, 0, 0),
                                        unselectedColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Отключить автосинхронизацию",
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                item {
                    if (selectedFrequency != AlarmSyncScheduler.Frequency.DISABLED) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(44, 44, 51)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Время синхронизации",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(61, 61, 70),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = String.format(
                                            "%02d:%02d",
                                            selectedHour,
                                            selectedMinute
                                        ),
                                        fontSize = 20.sp
                                    )
                                }

                                Text(
                                    text = "Рекомендуется выбирать ночное время,\nкогда устройство заряжается и подключено к Wi-Fi",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (selectedFrequency == AlarmSyncScheduler.Frequency.DISABLED) {
                                viewModel.cancelScheduledSync()
                            } else {
                                viewModel.scheduleSync(selectedFrequency, selectedHour, selectedMinute)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(139, 0, 0),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedFrequency == AlarmSyncScheduler.Frequency.DISABLED)
                                "Отключить"
                            else
                                "Сохранить расписание",
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // TimePicker Dialog
            if (showTimePicker) {
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                        showTimePicker = false
                    },
                    selectedHour,
                    selectedMinute,
                    true
                )

                LaunchedEffect(showTimePicker) {
                    timePickerDialog.show()
                }
            }
        }
    }
}