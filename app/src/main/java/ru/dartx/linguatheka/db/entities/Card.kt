package ru.dartx.linguatheka.db.entities

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
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "createTime")
    val createTime: String,
    @ColumnInfo(name = "remindTime")
    val remindTime: String,
    @ColumnInfo(name = "step")
    val step: Int
) : Serializable
