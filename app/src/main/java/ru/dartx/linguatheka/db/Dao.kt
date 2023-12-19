package ru.dartx.linguatheka.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.db.entities.Example

@Dao
interface Dao {
    @Query("SELECT * FROM cards ORDER BY remindTime, word ASC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id == :card_id")
    suspend fun getCardData(card_id: Int): Card

    @Query("SELECT * FROM example WHERE card_id IS :card_id ORDER BY finished, id ASC")
    suspend fun getExamplesByCardId(card_id: Int): List<Example>

    @Query(
        "SELECT * FROM cards WHERE word LIKE :cond OR examples LIKE :cond " +
                "OR translation LIKE :cond ORDER BY remindTime, word ASC"
    )
    suspend fun searchCards(cond: String): List<Card>

    @Query("SELECT * FROM cards WHERE word LIKE :cond AND id != :card_id")
    suspend fun findDuplicates(cond: String, card_id: Int): List<Card>

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
        deleteExamplesByCardId(card.id!!)
        updateCard(card)
        exampleList.forEach {
            insertExample(it)
        }
    }

    @Query("UPDATE example SET finished = :finished WHERE card_id = :cardId")
    suspend fun changeFinishedMarkForAllExamples(cardId: Int, finished: Boolean)

    @Query("SELECT lang FROM cards ORDER BY lang ASC")
    fun selectLang(): List<String>

    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}