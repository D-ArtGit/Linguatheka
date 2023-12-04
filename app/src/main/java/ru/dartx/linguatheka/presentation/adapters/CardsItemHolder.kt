package ru.dartx.linguatheka.presentation.adapters

import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.CardItemBinding
import ru.dartx.linguatheka.db.entities.Card
import ru.dartx.linguatheka.utils.TimeManager

class CardsItemHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    private val binding = CardItemBinding.bind(view)
    fun setData(card: Card, listener: CardAdapter.Listener) =
        with(binding) {
            if (card.remindTime == TimeManager.ENDLESS_FUTURE) {
                tvTime.visibility = View.GONE
                progressBar.visibility = View.GONE
                tvExamples.maxLines = 3
            }
            else {
                val tvTimeText: String =
                    view.context.getString(R.string.next_time_to_repeat) + TimeManager.getDateFormat(
                        card.remindTime
                    )
                tvTime.text = tvTimeText
                tvTime.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                tvExamples.maxLines = 1
            }
            if (TimeManager.isTimeToSetNewRemind(card.remindTime)) {
                tvWord.text = SpannableStringBuilder().bold { append(card.word) }
                tvExamples.text =
                    SpannableStringBuilder().bold { append(card.examples) }
                val value = TypedValue()
                view.context.theme.resolveAttribute(R.attr.coloredText, value, true)
                tvTime.setTextColor(value.data)
            } else {
                tvWord.text = card.word
                tvExamples.text = card.examples
                val value = TypedValue()
                view.context.theme.resolveAttribute(R.attr.secondaryText, value, true)
                tvTime.setTextColor(value.data)
            }
            progressBar.max = 9
            progressBar.progress = card.step
            itemView.setOnClickListener {
                listener.onClickCard(card)
            }
        }

    companion object {
        fun create(parent: ViewGroup): CardsItemHolder {
            return CardsItemHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_item, parent, false)
            )
        }
    }
}