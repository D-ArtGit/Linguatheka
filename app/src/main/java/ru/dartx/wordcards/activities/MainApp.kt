package ru.dartx.wordcards.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import ru.dartx.wordcards.db.MainDataBase

class MainApp : Application() {
    val database by lazy { MainDataBase.getDataBase(this) }
/*    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
    }*/
}