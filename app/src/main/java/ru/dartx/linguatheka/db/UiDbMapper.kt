package ru.dartx.linguatheka.db

import android.text.Spanned
import ru.dartx.linguatheka.db.entities.Example
import ru.dartx.linguatheka.domain.ExampleItemUiState
import ru.dartx.linguatheka.utils.HtmlManager

class UiDbMapper {
    fun mapExampleToExampleItemUiState(example: Example) = ExampleItemUiState(
        id = example.id,
        cardId = example.cardId,
        example = HtmlManager.getFromHtml(example.example).trim() as Spanned,
        translation = HtmlManager.getFromHtml(example.translation).trim() as Spanned,
        finished = example.finished,
        dividerVisibility = false,
        error = null,
        requestFocus = false,
        editMode = false
    )

    private fun mapExampleItemUiStateToExample(exampleItemUiState: ExampleItemUiState) = Example(
        exampleItemUiState.id,
        exampleItemUiState.cardId,
        HtmlManager.toHtml(exampleItemUiState.example).trim(),
        HtmlManager.toHtml(exampleItemUiState.translation).trim(),
        exampleItemUiState.finished
    )

    fun mapExampleItemUiStateListToExampleList(exampleItemUiStateList: List<ExampleItemUiState>) = exampleItemUiStateList.map {
        mapExampleItemUiStateToExample(it)
    }
}