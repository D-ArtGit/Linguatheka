package ru.dartx.wordcards.utils

import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

object TimeManager {
    private const val DEF_TIME_FORMAT = "yyyy.MM.dd - hh:mm:ss"
    private const val RUS_TIME_FORMAT = "hh:mm:ss - dd.MM.yyyy"

    fun getCurrentTime(): String {
        val date = LocalDateTime.now()
        return date.toString()
    }

    fun getTimeFormat(time: String): String {
        val defDate = LocalDateTime.parse(time)
        val formatter = DateTimeFormatter.ofPattern(RUS_TIME_FORMAT)
        return if (defDate != null) defDate.format(formatter)
        else time
    }

    fun addDays(time: String, days: Int): String {
        val defDate = LocalDateTime.parse(time)
        return defDate.plusDays(days.toLong()).toString()
    }
}