package ru.dartx.linguatheka.db

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.entities.Card

class MainViewModel(dataBase: MainDataBase) : ViewModel() {
    private val dao = dataBase.getDao()
    val allCards: LiveData<List<Card>> = dao.getAllCards().asLiveData()
    val foundCards = MutableLiveData<List<Card>>()

    fun searchCard(cond: String) =
        viewModelScope.launch { foundCards.postValue(dao.searchCards(cond)) }

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