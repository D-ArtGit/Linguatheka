package ru.dartx.linguatheka.presentation.adapters

import androidx.recyclerview.widget.DiffUtil
import ru.dartx.linguatheka.db.entities.Card

class CardsDiffCallback : DiffUtil.ItemCallback<Card>() {
    override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem == newItem
    }
}