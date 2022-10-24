package ru.dartx.wordcards.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object TimeManager {
    private const val DATE_FORMAT = "yyyy.MM.dd"
    const val ENDLESS_FUTURE = "2999-12-31T00:00:00.000"

    fun getCurrentTime(): String {
        val date = LocalDateTime.now()
        return date.toString()
    }

    fun getDateFormat(time: String): String {
        val defDate = LocalDateTime.parse(time)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        return if (defDate != null) defDate.format(formatter)
        else time
    }

    fun getTimeFormat(time: String): String {
        val defDate = LocalDateTime.parse(time)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        return if (defDate != null) defDate.format(formatter)
        else time
    }

    fun getTimeWithZone(time: String): String {
        val zonedUTC = ZonedDateTime.parse(time)
        val zonedLocal = zonedUTC.withZoneSameInstant(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        return zonedLocal.format(formatter)
    }

    fun isTimeToSetNewRemind(time: String): Boolean {
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