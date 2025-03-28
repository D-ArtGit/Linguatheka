package ru.dartx.linguatheka.domain

class GetCardDataUseCase(private val exampleListRepository: ExampleListRepository) {
    suspend operator fun invoke(cardId: Int): CardWithExamplesUiState {
        return exampleListRepository.getCardData(cardId)
    }
}