package ru.dartx.linguatheka.db

import android.app.Application
import androidx.preference.PreferenceManager
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.domain.CardWithExamplesUiState
import ru.dartx.linguatheka.domain.ExampleItemUiState
import ru.dartx.linguatheka.domain.ExampleListRepository
import ru.dartx.linguatheka.presentation.activities.MainApp
import ru.dartx.linguatheka.utils.LanguagesManager
import ru.dartx.linguatheka.utils.TimeManager

class ExampleListRepositoryImpl(application: Application) : ExampleListRepository {
    private val dataBase = MainDataBase.getDataBase(application as MainApp)
    private val dao = dataBase.getDao()
    private val exampleItemUiStateList =
        arrayListOf<ExampleItemUiState>()
    private val daysArray = application.resources.getIntArray(R.array.remind_days)
    private val defPreference = PreferenceManager.getDefaultSharedPreferences(application)
    private val defLang = defPreference.getString("def_lang", "en").toString()

    override suspend fun getCardData(cardId: Int): CardWithExamplesUiState {
        val card = dao.getCardData(cardId)
        val langArray = LanguagesManager.getLanguages()
        var langIndex = langArray[0].indexOf(card.lang)
        if (langIndex < 0) langIndex = langArray[0].indexOf(defLang)
        val cardLangText = langArray[1][langIndex]
        val needCheck = TimeManager.isTimeToSetNewRemind(card.remindTime)
        val tempExampleList = dao.getExamplesByCardId(cardId)
        var shouldShowDivider = false
        if (tempExampleList.indexOfFirst { it.finished } >= 0 &&
            tempExampleList.indexOfFirst { !it.finished } >= 0) {
            shouldShowDivider = true
        }
        tempExampleList.forEachIndexed { index, example ->
            val exampleItem = UiDbMapper().mapExampleToExampleItemUiState(example)
            exampleItem.itemNumber = index + 1
            if (index > 0 && shouldShowDivider && exampleItem.finished) {
                exampleItem.dividerVisibility = true
                shouldShowDivider = false
            }
            exampleItemUiStateList.add(exampleItem)
        }

        return CardWithExamplesUiState(
            isNeedCheck = needCheck,
            card = card,
            exampleList = exampleItemUiStateList as List<ExampleItemUiState>,
            cardLangText = cardLangText
        )
    }

    override suspend fun saveCard(cardWithExamplesUiState: CardWithExamplesUiState) {
        if (cardWithExamplesUiState.card.id == 0) {
            val card = cardWithExamplesUiState.card.copy(id = null)
            val exampleList =
                UiDbMapper().mapExampleItemUiStateListToExampleList(cardWithExamplesUiState.exampleList)
            dao.insertCard(card, exampleList)
        } else {
            val card = cardWithExamplesUiState.card
            val exampleList =
                UiDbMapper().mapExampleItemUiStateListToExampleList(cardWithExamplesUiState.exampleList)
            dao.updateCardWithItems(card, exampleList)
            if (cardWithExamplesUiState.isNeedCheck) {
                completeStep(card.id!!)
            }
        }
    }

    override suspend fun deleteCard(cardId: Int) {
        dao.deleteCard(cardId)
    }

    override suspend fun completeStep(cardId: Int) {
        val card = dao.getCardData(cardId)
        val step = card.step + 1
        val remindTime = if (step <= 8) {
            TimeManager.addDays(TimeManager.getCurrentTime(), daysArray[card.step])
        } else {
            dao.changeFinishedMarkForAllExamples(card.id!!, true)
            TimeManager.ENDLESS_FUTURE
        }
        val cardToUpdate = card.copy(step = step, remindTime = remindTime)
        dao.updateCard(cardToUpdate)
    }

    override suspend fun hasDuplicates(word: String, cardId: Int): Boolean {
        return dao.findDuplicates(word, cardId).isNotEmpty()
    }
}