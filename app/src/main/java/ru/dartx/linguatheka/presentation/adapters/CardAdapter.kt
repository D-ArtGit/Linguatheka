package ru.dartx.linguatheka.presentation.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.dartx.linguatheka.entities.Card

class CardAdapter(private val listener: Listener) :
    ListAdapter<Card, CardsItemHolder>(CardsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardsItemHolder {
        return CardsItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: CardsItemHolder, position: Int) {
        holder.setData(getItem(position), listener)
    }

    interface Listener {
        fun onClickCard(card: Card)
    }
}