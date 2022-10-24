package ru.dartx.wordcards.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dartx.wordcards.entities.Card

@Database(entities = [Card::class], version = 1, exportSchema = true)
abstract class MainDataBase : RoomDatabase() {
    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null
        fun getDataBase(context: Context): MainDataBase {
            Log.d("DArtX", "Instance open $INSTANCE")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "wordcards.db"
                ).build()
                Log.d("DArtX", "Instance rebuild $instance")
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            Log.d("DArtX", "Try instance close $INSTANCE")
            if (INSTANCE != null) {
                if (INSTANCE!!.isOpen) {
                    Log.d("DArtX", "Instance close")
                    INSTANCE!!.close()
                }
                INSTANCE = null
            }
        }
    }
}