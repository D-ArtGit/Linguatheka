package ru.dartx.wordcards.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey(autoGenerate = false)
    val id: Int? = 0,
    @ColumnInfo(name = "showHelp")
    val showHelp: Boolean

)
