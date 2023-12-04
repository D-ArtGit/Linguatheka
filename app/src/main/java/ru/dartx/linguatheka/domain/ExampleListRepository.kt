package ru.dartx.linguatheka.domain

interface ExampleListRepository {

    suspend fun getCardData(cardId: Int): CardWithExamplesUiState

    suspend fun saveCard(cardWithExamplesUiState: CardWithExamplesUiState)

    suspend fun deleteCard(cardId: Int)

    suspend fun completeStep(cardId: Int)

    suspend fun hasDuplicates(word: String, cardId: Int): Boolean
}