package ru.dartx.wordcards.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.dartx.wordcards.entities.Card

@Dao
interface Dao {
    @Query("SELECT * FROM cards")
    fun getAllCards(): Flow<List<Card>>
    @Query("DELETE FROM cards WHERE id IS :id")
    suspend fun deleteCard(id: Int)
    @Insert
    suspend fun insertCard(card: Card)
    @Update
    suspend fun updateCard(card: Card)
}