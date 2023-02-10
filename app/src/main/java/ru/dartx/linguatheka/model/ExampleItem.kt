package ru.dartx.linguatheka.model

import android.text.Spanned


data class ExampleItem(
    var id: Int?,
    var card_id: Int,
    var example: Spanned,
    var translation: Spanned,
    var tvExampleVisibility: Int,
    var edVisibility: Int,
    var dividerVisibility: Boolean,
    var error: String?,
    var requestFocus: Boolean,
    var finished: Boolean
)
