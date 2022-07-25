package ru.dartx.wordcards.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dartx.wordcards.entities.Card

@Database(entities = [Card::class], version = 1, exportSchema = true)
abstract class MainDataBase: RoomDatabase(){
    abstract fun getDao(): Dao
    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null
        fun getDataBase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "wordcards.db"
                ).build()
                instance
            }
        }
    }
}