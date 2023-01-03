package ru.dartx.linguatheka.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.workers.NotificationsWorker
import java.util.concurrent.TimeUnit

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
        startNotificationsWorker()
        if (defPreference.getBoolean("auto_backup", false)) {
            BackupAndRestoreManager.startBackupWorker(applicationContext)
        }
    }

    private fun startNotificationsWorker() {
        val notificationsRequest =
            PeriodicWorkRequestBuilder<NotificationsWorker>(
                30, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
            )
                .addTag("notifications")
                .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationsRequest
        )
    }
}