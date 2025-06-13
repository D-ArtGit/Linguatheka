package ru.dartx.linguatheka.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.db.ExampleListRepositoryImpl
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.domain.CardWithExamplesUiState
import ru.dartx.linguatheka.domain.CheckIfHasDuplicatesUseCase
import ru.dartx.linguatheka.domain.CompleteStepUseCase
import ru.dartx.linguatheka.domain.DeleteCardUseCase
import ru.dartx.linguatheka.domain.ExampleItemUiState
import ru.dartx.linguatheka.domain.GetCardDataUseCase
import ru.dartx.linguatheka.domain.SaveCardUseCase
import ru.dartx.linguatheka.utils.ComposingSpansRemover
import ru.dartx.linguatheka.utils.HtmlManager
import ru.dartx.linguatheka.utils.LanguagesManager
import ru.dartx.linguatheka.utils.TimeManager.addDays
import ru.dartx.linguatheka.utils.TimeManager.getCurrentTime

class CardViewModel(val application: Application, cardId: Int) : ViewModel() {
    private val repository = ExampleListRepositoryImpl(application)
    private val getCardDataUseCase = GetCardDataUseCase(repository)
    private val completeStepUseCase = CompleteStepUseCase(repository)
    private val checkIfHasDuplicatesUseCase = CheckIfHasDuplicatesUseCase(repository)
    private val saveCardUseCase = SaveCardUseCase(repository)
    private val deleteCardUseCase = DeleteCardUseCase(repository)
    private val exampleList = arrayListOf<ExampleItemUiState>()
    private val daysArray = application.resources.getIntArray(R.array.remind_days)
    private val defPreference = PreferenceManager.getDefaultSharedPreferences(application)
    private val defLang = defPreference.getString("def_lang", "en").toString()

    private var _shouldCloseActivity = MutableLiveData<String>()
    val shouldCloseActivity: LiveData<String>
        get() = _shouldCloseActivity
    private var _cardWithExamplesUiState = MutableStateFlow(CardWithExamplesUiState())
    val cardWithExamplesUiState: StateFlow<CardWithExamplesUiState> =
        _cardWithExamplesUiState.asStateFlow()

    private var _notifyItemInserted = MutableLiveData<Unit>()
    val notifyItemInserted: LiveData<Unit>
        get() = _notifyItemInserted

    private var _notifyItemChanged = MutableLiveData<List<Int>>()
    val notifyItemChanged: LiveData<List<Int>>
        get() = _notifyItemChanged
    private var _notifyItemRemoved = MutableLiveData<List<Int>>()
    val notifyItemRemoved: LiveData<List<Int>>
        get() = _notifyItemRemoved

    init {
        LanguagesManager.getUsedLanguages(application)
        if (cardId == 0) {
            newCard()
        } else {
            viewModelScope.launch {
                getCardData(cardId)
            }
        }
    }

    val langArray = LanguagesManager.getLanguages()

    private fun newCard() {
        val card = Card(
            0,
            defLang,
            "",
            "",
            "",
            getCurrentTime(),
            addDays(getCurrentTime(), daysArray[0]),
            0
        )
        addEmptyItem(false, null)
        _cardWithExamplesUiState.update {
            it.copy(
                card = card,
                isNeedCheck = false,
                exampleList = exampleList
            )
        }
    }

    fun addEmptyItem(needFocus: Boolean, error: String?) {
        val id = if (cardWithExamplesUiState.value.exampleList.isEmpty()) 1
        else cardWithExamplesUiState.value.exampleList.maxOf { it.id } + 1
        val exampleItemUiState = ExampleItemUiState(
            id,
            cardWithExamplesUiState.value.card.id ?: 0,
            HtmlManager.getFromHtml(""),
            HtmlManager.getFromHtml(""),
            false,
            error,
            requestFocus = needFocus,
            finished = false,
            editMode = true,
            itemNumber = exampleList.size + 1
        )
        exampleList.add(exampleItemUiState)
        _notifyItemInserted.value = Unit
    }

    fun deleteItem(exampleItemId: Int) {
        val indexToRemove = exampleList.indexOfFirst { it.id == exampleItemId }
        exampleList.removeAt(indexToRemove)
        setNumbersForItems()
        if (exampleList.isEmpty()) {
            addEmptyItem(true, application.getString(R.string.no_examples))
        } else {
            val indexToUpdate = arrayListOf<Int>()
            for (i in indexToRemove..exampleList.size) {
                indexToUpdate.add(i)
            }
            _notifyItemChanged.value = indexToUpdate
        }
    }

    private suspend fun getCardData(cardId: Int) {
        val tempCardUiState = getCardDataUseCase(cardId)
        exampleList.clear()
        exampleList.addAll(tempCardUiState.exampleList)
        _cardWithExamplesUiState.update {
            it.copy(
                card = tempCardUiState.card,
                exampleList = exampleList,
                cardLangText = tempCardUiState.cardLangText,
                isNeedCheck = tempCardUiState.isNeedCheck,
                inputWordError = null
            )
        }
    }

    fun setEditMode() {
        exampleList.forEach {
            it.editMode = true
        }
    }

    fun completeStep(cardId: Int) {
        viewModelScope.launch {
            completeStepUseCase(cardId)
            _shouldCloseActivity.value = application.getString(R.string.mark_done)
        }
    }

    fun resetProgress(isFullReset: Boolean) {
        val tmpCard = cardWithExamplesUiState.value.card.copy(
            step = 0,
            remindTime = addDays(getCurrentTime(), daysArray[0])
        )
        _cardWithExamplesUiState.update { it.copy(card = tmpCard, isNeedCheck = false) }
        if (isFullReset) {
            exampleList.forEach { it.finished = false }
            saveCard(tmpCard.word)
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            cardWithExamplesUiState.value.card.id?.let {
                deleteCardUseCase(it)
                _shouldCloseActivity.value = application.getString(R.string.card_deleted)
            }
        }
    }

    fun changeLang(langIndex: Int) {
        val tmpCard = _cardWithExamplesUiState.value.card.copy(lang = langArray[0][langIndex])
        val cardLangText = langArray[1][langIndex]
        _cardWithExamplesUiState.update { it.copy(card = tmpCard, cardLangText = cardLangText) }
    }

    fun saveCard(inputWord: String?) {
        viewModelScope.launch {
            val word = inputWord?.trim() ?: ""
            if (validateInputWord(word) && validateExamples()) {
                var examplesText = ""
                var translationText = ""
                cardWithExamplesUiState.value.exampleList.forEachIndexed { index, exampleItemUiState ->
                    exampleItemUiState.example =
                        ComposingSpansRemover.removeComposingSpans(exampleItemUiState.example)
                    exampleItemUiState.translation =
                        ComposingSpansRemover.removeComposingSpans(exampleItemUiState.translation)
                    if (index > 0) {
                        examplesText += "\n"
                        translationText += " "
                    }
                    examplesText += exampleItemUiState.example.toString().trim()
                    translationText += exampleItemUiState.translation.toString().trim()
                }
                val tmpCard = _cardWithExamplesUiState.value.card.copy(
                    word = word,
                    examples = examplesText,
                    translation = translationText
                )
                _cardWithExamplesUiState.update { it.copy(card = tmpCard) }
                saveCardUseCase(cardWithExamplesUiState.value)
                _shouldCloseActivity.value = application.getString(R.string.saved)
            }
        }

    }

    private suspend fun validateInputWord(word: String): Boolean {
        var result = true
        if (word.isBlank()) {
            _cardWithExamplesUiState.update { it.copy(inputWordError = application.getString(R.string.enter_word)) }
            result = false
        } else {
            cardWithExamplesUiState.value.card.id?.let { id ->
                if (checkIfHasDuplicatesUseCase(word, id)) {
                    _cardWithExamplesUiState.update {
                        it.copy(inputWordError = application.getString(R.string.word_exist))
                    }
                    result = false
                }
            }
        }
        return result
    }

    private fun validateExamples(): Boolean {
        val indexOfUpdated = sortedSetOf<Int>()
        val idToRemove = arrayListOf<Int>()
        val indexToRemove = arrayListOf<Int>()
        var result = true
        exampleList.forEachIndexed { index, exampleItemUiState ->
            if (exampleItemUiState.translation.isBlank() && exampleItemUiState.example.isBlank()) {
                idToRemove.add(exampleItemUiState.id)
                indexToRemove.add(index)
            }
        }
        if (idToRemove.isNotEmpty()) {
            idToRemove.forEach { id ->
                exampleList.removeIf { it.id == id }
            }
            setNumbersForItems()
            _notifyItemRemoved.value = indexToRemove
            for (i in idToRemove.min()..<exampleList.size) indexOfUpdated.add(i)
            if (exampleList.isEmpty()) {
                addEmptyItem(true, application.getString(R.string.no_examples))
                result = false
            }
        }
        if (result || exampleList.size > 1) {
            exampleList.forEachIndexed { index, exampleItemUiState ->
                if (exampleItemUiState.translation.isNotBlank() && exampleItemUiState.example.isBlank()) {
                    exampleItemUiState.error = application.getString(R.string.no_example)
                    indexOfUpdated.add(index)
                    result = false
                }
            }
        }
        if (!result) {
            _notifyItemChanged.value = indexOfUpdated.toList()
        }
        return result
    }

    fun resetInputWordError() {
        _cardWithExamplesUiState.update { it.copy(inputWordError = null) }
    }

    fun resetInputExampleError(exampleItemId: Int) {
        val index = exampleList.indexOfFirst { it.id == exampleItemId }
        if (index >= 0) {
            exampleList[index].error = null
            _notifyItemChanged.value = listOf(index)
        }
    }

    fun resetRequestFocus(exampleItemId: Int) {
        val index = exampleList.indexOfFirst { it.id == exampleItemId }
        exampleList[index].requestFocus = false
    }

    private fun setNumbersForItems() {
        exampleList.forEachIndexed { index, exampleItem ->
            exampleItem.itemNumber = index + 1
        }
    }
}