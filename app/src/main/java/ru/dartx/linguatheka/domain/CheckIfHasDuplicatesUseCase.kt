package ru.dartx.linguatheka.domain

class CheckIfHasDuplicatesUseCase(private val exampleListRepository: ExampleListRepository) {
    suspend operator fun invoke(word: String, cardId: Int): Boolean {
        return exampleListRepository.hasDuplicates(word, cardId)
    }
}