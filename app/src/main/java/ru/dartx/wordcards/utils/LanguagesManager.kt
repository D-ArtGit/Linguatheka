package ru.dartx.wordcards.utils

import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import java.util.*

object LanguagesManager {
    private val lang = Locale.getAvailableLocales()
    private var currentLang = ""
    private var langEntries = emptyArray<String>()
    private var langValues = emptyArray<String>()
    private var langArray = emptyArray<Array<String>>()
    fun getLanguages(): Array<Array<String>> {
        if (langArray.isNotEmpty()) return langArray
        for (i in lang.indices) {
            if (currentLang != lang[i].language) {
                currentLang = lang[i].language
                langValues += lang[i].language
                langEntries += "${lang[i].language} - ${lang[i].displayLanguage}"
            }
        }
        langArray += langValues
        langArray += langEntries
        Log.d("DArtX", "langArray = ${langArray.indices}")
        return langArray
    }

}