package ru.dartx.linguatheka.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.db.entities.Example

@Database(entities = [Card::class, Example::class], version = 1, exportSchema = true)
abstract class MainDataBase : RoomDatabase() {
    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null
        private val LOCK = Any()
        private const val DB_NAME = "linguatheka.db"
        private const val NEW_DB_FROM_ASSET = "new.db"
        fun getDataBase(context: Context): MainDataBase {

            INSTANCE?.let { return it }
            synchronized(LOCK) {
                INSTANCE?.let { return it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    DB_NAME
                )
                    .createFromAsset(NEW_DB_FROM_ASSET)
                    .build()
                INSTANCE = instance
                return instance
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