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
    private const val RUS_DATE_FORMAT = "d MMM yyyy"
    private const val RUS_TIME_FORMAT = "HH:mm:ss - dd.MM.yyyy"
    private const val DATE_FORMAT = "yyyy.MM.dd"
    const val ENDLESS_FUTURE = "2999-12-31T00:00:00.000"

    fun getCurrentTime(): String {
        val date = LocalDateTime.now()
        return date.toString()
    }

    fun getTimeFormat(time: String): String {
        val defDate = LocalDateTime.parse(time)
        val formatter = DateTimeFormatter.ofPattern(RUS_DATE_FORMAT)
        return if (defDate != null) defDate.format(formatter)
        else time
    }

    fun isTimeToSetNewRemind (time: String): Boolean {
        val dateFromTime = LocalDateTime.parse(time)
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
        val dateOfRemind = dateFromTime.format(formatter)
        val currentDateTime = LocalDateTime.now()
        val currentDate = currentDateTime.format(formatter)
        return dateOfRemind <= currentDate
    }

    fun addDays(time: String, days: Int): String {
        val defDate = LocalDateTime.parse(time)
        return defDate.plusDays(days.toLong()).toString()
    }

    fun addHours(time: String, hours: Int): String {
        val defDate = LocalDateTime.parse(time)
        return defDate.plusHours(hours.toLong()).toString()
    }
}