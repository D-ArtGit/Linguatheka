package ru.dartx.wordcards.utils

import android.content.Context
import android.util.Pair
import ru.dartx.wordcards.db.MainDataBase
import java.util.*
import kotlin.concurrent.thread

object LanguagesManager {
    private var langArray = emptyArray<Array<String>>()
    private var usedLanguages = ArrayList<String>()

    fun getUsedLanguages(context: Context) {
        thread {
            val database = MainDataBase.getDataBase(context).getDao()
            val usedLanguagesTmp: ArrayList<String> = database.selectLang() as ArrayList<String>
            val hashedLanguages = HashSet<String>()
            hashedLanguages.addAll(usedLanguagesTmp)
            usedLanguagesTmp.clear()
            usedLanguagesTmp.addAll(hashedLanguages)
            usedLanguagesTmp.sort()
            if (usedLanguages.size != usedLanguagesTmp.size) {
                langArray = emptyArray()
            }
            usedLanguages = usedLanguagesTmp
        }
    }

    fun getLanguages(): Array<Array<String>> {
        if (langArray.isNotEmpty()) return langArray
        val lang = Locale.getAvailableLocales()
        var currentLang = ""
        val tmpLangList: ArrayList<Pair<String, String>> = ArrayList()
        val finalLangList: ArrayList<Pair<String, String>> = ArrayList()
        var langEntries = emptyArray<String>()
        var langValues = emptyArray<String>()

        for (i in lang.indices) {
            if (currentLang != lang[i].language) {
                currentLang = lang[i].language
                when (usedLanguages.contains(lang[i].language)) {
                    true -> finalLangList.add(
                        Pair(
                            lang[i].language,
                            lang[i].displayLanguage
                        )
                    )
                    false -> tmpLangList.add(
                        Pair(
                            lang[i].language,
                            lang[i].displayLanguage
                        )
                    )
                }
            }
        }
        tmpLangList.sortBy { it.second }
        finalLangList.addAll(tmpLangList)
        for (i in finalLangList.indices) {
            langValues += finalLangList[i].first
            langEntries += finalLangList[i].second
        }
        langArray += langValues
        langArray += langEntries
        return langArray
    }
}