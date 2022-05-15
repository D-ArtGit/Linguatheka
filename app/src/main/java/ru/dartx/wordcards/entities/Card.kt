package ru.dartx.wordcards.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Int?,
    @ColumnInfo(name = "lang")
    val lang: String,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "examples")
    val examples: String,
    @ColumnInfo(name = "examples_html")
    val examples_html: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "translation_html")
    val translation_html: String,
    @ColumnInfo(name = "createDate")
    val createTime: String,
    @ColumnInfo(name = "remindDate")
    val remindTime: String,
    @ColumnInfo(name = "step")
    val step: Int
) : Serializable
