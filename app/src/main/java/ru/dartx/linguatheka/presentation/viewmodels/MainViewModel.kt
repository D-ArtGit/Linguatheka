package ru.dartx.linguatheka.presentation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.db.entities.Card

class MainViewModel(dataBase: MainDataBase) : ViewModel() {
    private val dao = dataBase.getDao()
    val allCards: LiveData<List<Card>> = dao.getAllCards().asLiveData()
    val foundCards = MutableLiveData<List<Card>>()

    fun searchCard(cond: String) =
        viewModelScope.launch { foundCards.postValue(dao.searchCards(cond)) }

    fun updateCard(card: Card) = viewModelScope.launch { dao.updateCard(card) }

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