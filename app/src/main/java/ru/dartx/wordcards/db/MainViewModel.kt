package ru.dartx.wordcards.db

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.dartx.wordcards.entities.Card

class MainViewModel(dataBase: MainDataBase) : ViewModel() {
    private val dao = dataBase.getDao()
    val allCards: LiveData<List<Card>> = dao.getAllCards().asLiveData()
    val foundCards = MutableLiveData<List<Card>>()
    val notificationCard = MutableLiveData<List<Card>>()

    fun searchCard(cond: String) =
        viewModelScope.launch { foundCards.postValue(dao.searchCards(cond)) }

    fun notificationCards(cond: String) =
        viewModelScope.launch { notificationCard.postValue(dao.notificationCards(cond)) }

    fun insertCard(card: Card) = viewModelScope.launch { dao.insertCard(card) }
    fun updateCard(card: Card) = viewModelScope.launch { dao.updateCard(card) }
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