package ru.dartx.linguatheka.db

import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow
import ru.dartx.linguatheka.entities.Card
import ru.dartx.linguatheka.entities.Example

@Dao
interface Dao {
    @Query("SELECT * FROM cards ORDER BY remindTime, word ASC")
    fun getAllCards(): Flow<List<Card>>

    @Query(
        "SELECT * FROM cards WHERE word LIKE :cond OR examples LIKE :cond " +
                "OR translation LIKE :cond ORDER BY remindTime, word ASC"
    )
    suspend fun searchCards(cond: String): List<Card>

    @Query("SELECT * FROM cards WHERE word IS :cond AND id != :card_id")
    suspend fun findDuplicates(cond: String, card_id: Int): List<Card>

    @Query("SELECT * FROM example WHERE card_id IS :card_id ORDER BY finished, id ASC")
    fun findExamplesByCardId(card_id: Int): List<Example>

    @Query(
        "SELECT * FROM cards WHERE remindTime <= :cond " +
                "AND step < 9 ORDER BY remindTime, word ASC"
    )
    fun notificationCards(cond: String): List<Card>

    @Query("DELETE FROM cards WHERE id IS :id")
    suspend fun deleteOnlyCard(id: Int)

    @Query("DELETE FROM example WHERE card_id IS :id")
    suspend fun deleteExamplesByCardId(id: Int)

    @Transaction
    suspend fun deleteCard(id: Int) {
        deleteExamplesByCardId(id)
        deleteOnlyCard(id)
    }

    @Insert
    suspend fun insertOnlyCard(card: Card): Long

    @Insert
    suspend fun insertExample(example: Example)

    @Transaction
    suspend fun insertCard(card: Card, exampleList: List<Example>) {
        val cardId = insertOnlyCard(card).toInt()
        exampleList.forEach {
            insertExample(
                it.copy(
                    id = cardId * 100 + it.id,
                    card_id = cardId
                )
            )
        }
    }

    @Update
    suspend fun updateCard(card: Card)

    @Transaction
    suspend fun updateCardWithItems(card: Card, exampleList: List<Example>) {
        updateCard(card)
        deleteExamplesByCardId(card.id!!)
        exampleList.forEach {
            insertExample(
                it.copy(
                    id = card.id * 100 + it.id,
                    card_id = card.id
                )
            )
        }
    }

    @Update
    suspend fun updateExample(example: Example)

    @Query("SELECT lang FROM cards ORDER BY lang ASC")
    fun selectLang(): List<String>
}