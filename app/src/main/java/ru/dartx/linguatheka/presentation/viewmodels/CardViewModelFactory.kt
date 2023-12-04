package ru.dartx.linguatheka.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CardViewModelFactory(
    private val application: Application,
    private val cardId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardViewModel::class.java))
            return CardViewModel(application, cardId) as T
        throw RuntimeException("Unknown view model class $modelClass")
    }
}