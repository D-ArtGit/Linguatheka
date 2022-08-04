package ru.dartx.wordcards.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.db.MainDataBase

class MainApp : Application() {
    val database by lazy { MainDataBase.getDataBase(this) }
    override fun onCreate() {
        super.onCreate()
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val value = defPreference.getString("night_mode", "system")
        AppCompatDelegate.setDefaultNightMode(
            when (value) {
                "day" -> AppCompatDelegate.MODE_NIGHT_NO
                "night" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}