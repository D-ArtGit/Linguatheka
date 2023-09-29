package ru.dartx.linguatheka.presentation.viewmodels

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.entities.Card
import ru.dartx.linguatheka.entities.Example

class MainViewModel(dataBase: MainDataBase) : ViewModel() {
    private val dao = dataBase.getDao()
    val allCards: LiveData<List<Card>> = dao.getAllCards().asLiveData()
    val foundCards = MutableLiveData<List<Card>>()

    fun searchCard(cond: String) =
        viewModelScope.launch { foundCards.postValue(dao.searchCards(cond)) }

    fun findExampleByCardId(card_id: Int): List<Example> =
        dao.findExamplesByCardId(card_id)

    suspend fun findDuplicates(cond: String, card_id: Int): List<Card> = dao.findDuplicates(cond, card_id)

    fun insertCard(card: Card, exampleList: List<Example>) =
        viewModelScope.launch { dao.insertCard(card, exampleList) }

    fun updateCard(card: Card) = viewModelScope.launch { dao.updateCard(card) }
    fun updateCardWithItems(card: Card, exampleList: List<Example>) =
        viewModelScope.launch { dao.updateCardWithItems(card, exampleList) }

    fun deleteCard(id: Int) = viewModelScope.launch { dao.deleteCard(id) }

    class MainViewModelFactory(private val database: MainDataBase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModelClass")
        }

    }
}