package ru.dartx.wordcards.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo(name = "lang")
    val lang: String,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "createDate")
    val createTime: String,
    @ColumnInfo(name = "remindDate")
    val remindTime: String,
    @ColumnInfo(name = "step")
    val step: Int
)
