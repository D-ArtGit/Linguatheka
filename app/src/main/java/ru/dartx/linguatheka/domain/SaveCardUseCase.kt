package ru.dartx.linguatheka.domain

class SaveCardUseCase(private val exampleListRepository: ExampleListRepository) {
    suspend operator fun invoke(cardWithExampleUiState: CardWithExamplesUiState) {
        exampleListRepository.saveCard(cardWithExampleUiState)
    }
}