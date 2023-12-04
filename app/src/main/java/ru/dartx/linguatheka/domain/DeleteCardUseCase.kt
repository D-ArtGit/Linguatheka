package ru.dartx.linguatheka.domain

class DeleteCardUseCase(private val exampleListRepository: ExampleListRepository) {
    suspend operator fun invoke(cardId: Int) {
        exampleListRepository.deleteCard(cardId)
    }
}