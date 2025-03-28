package ru.dartx.linguatheka.domain

class CompleteStepUseCase(private val exampleListRepository: ExampleListRepository) {
    suspend operator fun invoke(cardId: Int) {
        exampleListRepository.completeStep(cardId)
    }
}