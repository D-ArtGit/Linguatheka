package ru.dartx.wordcards.db

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.CardItemBinding
import ru.dartx.wordcards.entities.Card
import ru.dartx.wordcards.utils.HtmlManager
import ru.dartx.wordcards.utils.TimeManager

class CardAdapter(private val listener: Listener) :
    ListAdapter<Card, CardAdapter.ItemHolder>(ItemComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener)
    }

    class ItemHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val binding = CardItemBinding.bind(view)
        fun setData(card: Card, listener: Listener) =
            with(binding) {
                if (card.remindTime == TimeManager.ENDLESS_FUTURE) {
                    tvTime.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    tvExamples.maxLines = 3
                }
                else {
                    val tvTimeText: String =
                        view.context.getString(R.string.next_time_to_repeat) + TimeManager.getTimeFormat(
                            card.remindTime
                        )
                    tvTime.text = tvTimeText
                }
                if (TimeManager.isTimeToSetNewRemind(card.remindTime)) {
                    tvWord.text = SpannableStringBuilder().bold { append(card.word) }
                    tvExamples.text =
                        SpannableStringBuilder().bold { append(HtmlManager.getFromHtml(card.examples)) }
                } else {
                    tvWord.text = card.word
                    tvExamples.text = HtmlManager.getFromHtml(card.examples).toString()
                }
                progressBar.max = 9
                progressBar.progress = card.step
                itemView.setOnClickListener {
                    listener.onClickCard(card)
                }
            }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.card_item, parent, false)
                )
            }
        }
    }

    class ItemComparator : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem == newItem
        }

    }

    interface Listener {
        fun onClickCard(card: Card)
    }
}