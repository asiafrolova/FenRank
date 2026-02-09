package com.example.fencing_project.view

import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.work.SyncServiceManager
import com.example.fencing_project.viewmodel.SyncViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(navController: NavController, pref: SharedPrefsManager) {
    val viewModel: SyncViewModel = hiltViewModel()
    val context = LocalContext.current

    var selectedFrequency by remember {
        mutableStateOf(SyncServiceManager.ScheduleFrequency.DISABLED)
    }
    var selectedHour by remember { mutableStateOf(2) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val schedule = viewModel.getCurrentSchedule()
        schedule?.let {
            selectedFrequency = it.frequency
            selectedHour = it.hour
            selectedMinute = it.minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = getString(context,R.string.schedule_sync,pref.getLanguage()),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = getString(context,R.string.back,pref.getLanguage()),
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
                    FrequencySelectionCard(
                        selectedFrequency = selectedFrequency,
                        onFrequencySelected = { frequency ->
                            selectedFrequency = frequency
                        },
                        context=context,
                        pref=pref
                    )
                }


                item {
                    if (selectedFrequency != SyncServiceManager.ScheduleFrequency.DISABLED) {
                        TimeSelectionCard(
                            selectedHour = selectedHour,
                            selectedMinute = selectedMinute,
                            onTimeClick = { showTimePicker = true },
                            context=context,
                            pref=pref
                        )
                    }
                }


                item {
                    Button(
                        onClick = { if (selectedFrequency == SyncServiceManager.ScheduleFrequency.DISABLED) {
                            viewModel.cancelScheduledSync()
                        } else {
                            viewModel.setupSyncSchedule(pref.getUserId()?:"",selectedFrequency,selectedHour,selectedMinute)

                        }
                            navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(139, 0, 0),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedFrequency == SyncServiceManager.ScheduleFrequency.DISABLED) {
                                getString(context,R.string.off,pref.getLanguage())
                            } else {
                                getString(context,R.string.save_schedule,pref.getLanguage())
                            },
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (showTimePicker) {
                TimePickerDialog(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onTimeSelected = { hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false }
                )
            }
        }
    }
}

@Composable
private fun FrequencySelectionCard(
    selectedFrequency: SyncServiceManager.ScheduleFrequency,
    onFrequencySelected: (SyncServiceManager.ScheduleFrequency) -> Unit,
    context: Context,
    pref: SharedPrefsManager
) {
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
                text = getString(context,R.string.period,pref.getLanguage()),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            SyncServiceManager.ScheduleFrequency.values()
                .filter { it != SyncServiceManager.ScheduleFrequency.DISABLED }
                .forEach { frequency ->
                    FrequencyRadioItem(
                        frequency = frequency,
                        isSelected = selectedFrequency == frequency,
                        onClick = { onFrequencySelected(frequency) },
                        context=context,
                        pref=pref
                    )
                }

            FrequencyRadioItem(
                frequency = SyncServiceManager.ScheduleFrequency.DISABLED,
                isSelected = selectedFrequency == SyncServiceManager.ScheduleFrequency.DISABLED,
                onClick = { onFrequencySelected(SyncServiceManager.ScheduleFrequency.DISABLED) },
                context=context,
                pref=pref
            )
        }
    }
}

@Composable
private fun FrequencyRadioItem(
    frequency: SyncServiceManager.ScheduleFrequency,
    isSelected: Boolean,
    onClick: () -> Unit,
    context: Context,
    pref: SharedPrefsManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(139, 0, 0),
                unselectedColor = Color.White
            )
        )
        var text = ""
        if (frequency.displayName=="Раз в день"){
            text =getString(context,R.string.once_a_day,pref.getLanguage())
        }else if(frequency.displayName=="Раз в неделю"){
            text =getString(context,R.string.once_a_week,pref.getLanguage())
        }else if(frequency.displayName=="Раз в месяц"){
            text= getString(context,R.string.once_a_month,pref.getLanguage())
        }else{
            text= getString(context,R.string.disabled,pref.getLanguage())
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp
        )

    }
}

@Composable
private fun TimeSelectionCard(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeClick: () -> Unit,
    context: Context,
    pref: SharedPrefsManager
) {
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
                text = getString(context,R.string.time_sync,pref.getLanguage()),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTimeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(61, 61, 70),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    fontSize = 20.sp
                )
            }

            Text(
                text = getString(context,R.string.recomend_sync,pref.getLanguage()),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}



@Composable
private fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                onTimeSelected(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.setOnDismissListener {
            onDismiss()
        }

        timePickerDialog.show()

    }
}