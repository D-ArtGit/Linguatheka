package ru.dartx.linguatheka.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.dartx.linguatheka.entities.Card

@Dao
interface Dao {
    @Query("SELECT * FROM cards ORDER BY remindTime, word ASC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE word LIKE :cond OR examples LIKE :cond " +
            "OR translation LIKE :cond ORDER BY remindTime, word ASC")
    suspend fun searchCards(cond: String): List<Card>

    @Query("SELECT * FROM cards WHERE remindTime <= :cond " +
            "AND step < 9 ORDER BY remindTime, word ASC")
    fun notificationCards(cond: String): List<Card>

    @Query("DELETE FROM cards WHERE id IS :id")
    suspend fun deleteCard(id: Int)

    @Insert
    suspend fun insertCard(card: Card)

    @Update
    suspend fun updateCard(card: Card)

    @Query("SELECT lang FROM cards ORDER BY lang ASC")
    fun selectLang(): List<String>
}