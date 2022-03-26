package ru.dartx.wordcards.activities

import android.app.Application
import ru.dartx.wordcards.db.MainDataBase

class MainApp : Application() {
    val database by lazy { MainDataBase.getDataBase(this) }
}