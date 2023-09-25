package ru.dartx.linguatheka.presentation.adapters

import androidx.recyclerview.widget.DiffUtil
import ru.dartx.linguatheka.domain.ExampleItem

class ExampleDiffCallback : DiffUtil.ItemCallback<ExampleItem>() {
    override fun areItemsTheSame(oldItem: ExampleItem, newItem: ExampleItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExampleItem, newItem: ExampleItem): Boolean {
        return oldItem == newItem
    }
}