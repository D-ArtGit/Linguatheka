package ru.dartx.wordcards.utils

import android.content.Context
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.R

object ThemeManager {
    fun getSelectedTheme(context: Context): Int {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        return when (defPreference.getString(
            "theme",
            "blue"
        )) {
            "blue" -> R.style.Theme_WordCardsBlue
            "green" -> R.style.Theme_WordCardsGreen
            else -> R.style.Theme_WordCardsRed
        }
    }

    fun getSelectedDialogTheme(context: Context): Int {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        return when (defPreference.getString(
            "theme",
            "blue"
        )) {
            "blue" -> R.style.Theme_WordCardsDialogBlue
            "green" -> R.style.Theme_WordCardsDialogGreen
            else -> R.style.Theme_WordCardsDialogRed
        }
    }

    fun getSelectedThemeNoBar(context: Context): Int {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        return when (defPreference.getString(
            "theme",
            "blue"
        )) {
            "blue" -> R.style.Theme_WordCardsBlue_NoActionBar
            "green" -> R.style.Theme_WordCardsGreen_NoActionBar
            else -> R.style.Theme_WordCardsRed_NoActionBar
        }
    }
}