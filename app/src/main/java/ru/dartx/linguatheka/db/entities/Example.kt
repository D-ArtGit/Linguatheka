package ru.dartx.linguatheka.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "example", indices = [Index(value = ["card_id"])])
data class Example(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    @ColumnInfo(name = "card_id")
    val cardId: Int,
    @ColumnInfo(name = "example")
    val example: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "finished")
    val finished: Boolean
) : Serializable
