package ru.dartx.linguatheka.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.db.entities.Example

@Database(
    entities = [Card::class, Example::class], version = 2, exportSchema = true
)
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
                    .addMigrations(MIGRATION_1_2)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
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

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE cards_new (id INTEGER PRIMARY KEY AUTOINCREMENT, lang TEXT NOT NULL, word TEXT NOT NULL, examples TEXT NOT NULL, translation TEXT NOT NULL, create_time TEXT NOT NULL, remind_time TEXT NOT NULL, step INTEGER NOT NULL)"
                )
                db.execSQL("INSERT INTO cards_new (id, lang, word, examples, translation, create_time, remind_time, step) SELECT id, lang, word, examples, translation, createTime, remindTime, step FROM cards")
                db.execSQL("DROP TABLE cards")
                db.execSQL("ALTER TABLE cards_new RENAME TO cards")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cards_word_examples_translation ON cards (word, examples, translation)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cards_remind_time_word ON cards (remind_time, word)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_lang ON cards (lang)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_example_card_id ON example (card_id)")
            }
        }
    }
}