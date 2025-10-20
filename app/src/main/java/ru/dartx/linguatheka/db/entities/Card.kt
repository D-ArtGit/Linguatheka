package ru.dartx.linguatheka.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["word", "examples", "translation"], unique = true),
        Index(value = ["remind_time", "word"], unique = true),
        Index(value = ["lang"])]
)
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
    @ColumnInfo(name = "create_time")
    val createTime: String,
    @ColumnInfo(name = "remind_time")
    val remindTime: String,
    @ColumnInfo(name = "step")
    val step: Int
) : Serializable
