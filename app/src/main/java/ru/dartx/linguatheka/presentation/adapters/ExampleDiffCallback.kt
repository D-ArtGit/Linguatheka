package ru.dartx.linguatheka.presentation.adapters

import androidx.recyclerview.widget.DiffUtil
import ru.dartx.linguatheka.domain.ExampleItemUiState

class ExampleDiffCallback : DiffUtil.ItemCallback<ExampleItemUiState>() {
    override fun areItemsTheSame(oldItem: ExampleItemUiState, newItem: ExampleItemUiState): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExampleItemUiState, newItem: ExampleItemUiState): Boolean {
        return oldItem == newItem
    }
}