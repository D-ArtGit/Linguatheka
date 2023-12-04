package ru.dartx.linguatheka.domain

import android.text.Spanned


data class ExampleItemUiState(
    var id: Int,
    var cardId: Int,
    var example: Spanned,
    var translation: Spanned,
    var dividerVisibility: Boolean,
    var error: String?,
    var requestFocus: Boolean,
    var finished: Boolean,
    var editMode: Boolean,
    var itemNumber: Int = 0
)
