package ru.dartx.linguatheka.domain

import ru.dartx.linguatheka.db.entities.Card

data class CardWithExamplesUiState(
    val card: Card = Card(
        0,
        "",
        "",
        "",
        "",
        "",
        "",
        0
    ),
    val exampleList: List<ExampleItemUiState> = listOf(),
    val cardLangText: String? = null,
    val isNeedCheck: Boolean = false,
    val inputWordError: String? = null
)
