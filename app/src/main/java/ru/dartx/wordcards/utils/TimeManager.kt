package ru.dartx.wordcards.utils

import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object TimeManager {
    private const val DEF_TIME_FORMAT = "yyyy.MM.dd - hh:mm:ss"
    private const val RUS_TIME_FORMAT = "hh:mm:ss - dd.MM.yyyy"

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    fun getTimeFormat(time: String): String {
        Log.d("DArtX", "Time: $time")
        val defFormatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault())
        val defDate = defFormatter.parse(time)
        val formatter = SimpleDateFormat(
            RUS_TIME_FORMAT,
            Locale.getDefault()
        )
        return if (defDate != null) formatter.format(defDate)
        else time
    }
}