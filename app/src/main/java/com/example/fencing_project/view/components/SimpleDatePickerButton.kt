import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.fencing_project.R
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager

@Composable
fun SimpleDatePickerButton(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager
) {
    val context = LocalContext.current
    val dateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy", Locale(pref.getLanguage()))
    }
    val formattedDate = remember(selectedDate) {
        dateFormatter.format(selectedDate)
    }

    Button(
        onClick = {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate
            }

            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val newCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    onDateSelected(newCalendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.show()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(44, 44, 51),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(getString(context,R.string.date_bout,pref.getLanguage()) +" $formattedDate", fontSize = 15.sp, modifier = Modifier.padding(5.dp))
            Icon(
                painter = painterResource(R.drawable.calendar),
                contentDescription = getString(context,R.string.choice_date,pref.getLanguage()),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

