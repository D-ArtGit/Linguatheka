package ru.dartx.linguatheka.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dartx.linguatheka.entities.Card

@Database(entities = [Card::class], version = 1, exportSchema = true)
abstract class MainDataBase : RoomDatabase() {
    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null
        fun getDataBase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "wordcards.db"
                ).createFromAsset("new.db")
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            if (INSTANCE != null) {
                if (INSTANCE!!.isOpen) {
                    INSTANCE!!.close()
                }
                INSTANCE = null
            }
        }
    }
}