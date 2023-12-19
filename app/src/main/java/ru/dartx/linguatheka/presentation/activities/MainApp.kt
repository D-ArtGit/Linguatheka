package ru.dartx.linguatheka.presentation.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.workers.BackupWorker
import ru.dartx.linguatheka.workers.NotificationsWorker

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
        NotificationsWorker.startNotificationsWorker(applicationContext)
        if (defPreference.getBoolean("auto_backup", false)) {
            BackupWorker.startBackupWorker(applicationContext)
        }
    }
}