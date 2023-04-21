package ru.dartx.linguatheka.db

import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.*
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ExampleItemBinding
import ru.dartx.linguatheka.model.ExampleItem
import ru.dartx.linguatheka.utils.Animations

class ExampleAdapter(
    private val exampleList: ArrayList<ExampleItem>
) :
    RecyclerView.Adapter<ExampleAdapter.ExampleViewHolder>() {
    inner class ExampleViewHolder(private val binding: ExampleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(exampleItem: ExampleItem) {
            binding.exampleItem = exampleItem
            with(binding) {
                "${tvExampleHeader.context.getString(R.string.example)} ${adapterPosition + 1}"
                    .also {
                        tvExampleHeader.text = it
                    }
                if (exampleItem.requestFocus) {
                    edExample.requestFocus()
                    exampleItem.requestFocus = false
                }
                if (exampleItem.error != null) {
                    tilExample.error = exampleItem.error
                    edExample.requestFocus()
                }
            }
            setListeners(binding, adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        return ExampleViewHolder(
            ExampleItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        holder.bind(exampleList[position])
    }

    override fun getItemCount(): Int {
        return exampleList.size
    }

    private fun getActionModeCallback(
        binding: ExampleItemBinding,
        selectedField: Int
    ): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return if (mode != null) {
                    mode.menuInflater.inflate(R.menu.selected_text_menu, menu)
                    true
                } else false
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return if (menu != null) {
                    selectedTextActionPrepare(menu)
                    true
                } else false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return if (item!!.itemId == R.id.bold) {
                    setBoldForSelectedText(binding, selectedField)
                    true
                } else false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {

            }
        }
    }

    private fun setBoldForSelectedText(
        binding: ExampleItemBinding,
        selectedField: Int
    ) {
        with(binding) {
            if (selectedField == EXAMPLE) {
                val startPos = edExample.selectionStart
                val endPos = edExample.selectionEnd
                val styles = edExample.text!!.getSpans(startPos, endPos, StyleSpan::class.java)
                var boldStyle: StyleSpan? = null
                if (styles.isNotEmpty()) edExample.text!!.removeSpan(styles[0])
                else boldStyle = StyleSpan(Typeface.BOLD)
                edExample.text!!.setSpan(
                    boldStyle,
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                edExample.text!!.trim()
                exampleItem?.example = edExample.text!!
                edExample.setSelection(startPos)

            } else if (selectedField == TRANSLATION) {
                val startPos = edTranslation.selectionStart
                val endPos = edTranslation.selectionEnd
                val styles = edTranslation.text!!.getSpans(startPos, endPos, StyleSpan::class.java)
                var boldStyle: StyleSpan? = null
                if (styles.isNotEmpty()) edTranslation.text!!.removeSpan(styles[0])
                else boldStyle = StyleSpan(Typeface.BOLD)
                edTranslation.text!!.setSpan(
                    boldStyle,
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                edTranslation.text!!.trim()
                exampleItem?.translation = edTranslation.text!!
                edTranslation.setSelection(startPos)
            }
        }
    }

    private fun selectedTextActionPrepare(menu: Menu) {
        while (menu.getItem(0).order < 50) {
            val item = menu.getItem(0)
            menu.removeItem(item.itemId)
            when (item.itemId) {
                android.R.id.paste -> addMenuItem(menu, item.groupId, item.itemId, 52, item.title)
                android.R.id.copy -> addMenuItem(menu, item.groupId, item.itemId, 53, item.title)
                android.R.id.cut -> addMenuItem(menu, item.groupId, item.itemId, 54, item.title)
                android.R.id.textAssist -> addMenuItem(
                    menu,
                    item.groupId,
                    item.itemId,
                    55,
                    item.title
                )
                else -> addMenuItem(menu, item.groupId, item.itemId, 100 + item.order, item.title)
            }
        }
    }

    private fun addMenuItem(
        menu: Menu,
        groupId: Int,
        itemId: Int,
        order: Int,
        title: CharSequence?
    ) {
        menu.add(groupId, itemId, order, title)
    }

    private fun setListeners(
        binding: ExampleItemBinding,
        adapterPosition: Int
    ) = with(binding) {
        edExample.customSelectionActionModeCallback =
            getActionModeCallback(binding, EXAMPLE)
        edTranslation.customSelectionActionModeCallback = getActionModeCallback(
            binding,
            TRANSLATION
        )
        edExample.setOnFocusChangeListener { view, hasFocus ->
            val editText = view as EditText
            if (!hasFocus) editText.clearComposingText()
        }
        if (exampleItem?.error != null) {
            edExample.addTextChangedListener { text ->
                if (!text.isNullOrEmpty()) {
                    exampleItem?.error = null
                    notifyItemChanged(adapterPosition)
                    edExample.setSelection(text.length)
                }
            }
        }
        edTranslation.setOnFocusChangeListener { view, hasFocus ->
            val editText = view as EditText
            if (!hasFocus) editText.clearComposingText()
        }
        tvExample.setOnClickListener {
            if (translationWrapper.visibility == View.GONE) {

                ivShowHide.animate().rotation(180F).start()

                /*translationWrapper.apply {
                    alpha = 0F
                    visibility = View.VISIBLE
                    translationY = -translationHeight / 2
                }*/

                /*val alphaAnim = ObjectAnimator.ofFloat(
                    translationWrapper,
                    "alpha",
                    1F
                ).apply {
                    duration = shortAnimationDuration
                }
                val translationYAnim = ObjectAnimator.ofFloat(
                    translationWrapper,
                    "translationY",
                    0F
                ).apply {
                    duration = shortAnimationDuration
                }
                AnimatorSet().apply {
                    interpolator = AccelerateInterpolator()
                    play(alphaAnim).with(translationYAnim)
                    start()
                }*/

                /*translationWrapper.animate()
                    .setDuration(shortAnimationDuration)
                    .setInterpolator(AccelerateInterpolator())
                    .translationY(0F)
                    .alpha(1F)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            translationWrapper.apply {
                                translationY = 0F
                                visibility = View.VISIBLE
                            }
                        }
                    })*/

                Animations.expand(translationWrapper)

            } else {
                ivShowHide.animate().rotation(0F).start()
                /*translationWrapper.animate()
                    .setDuration(shortAnimationDuration)
                    .setInterpolator(DecelerateInterpolator())
                    .translationY(-translationHeight / 2)
                    .alpha(0F)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            translationWrapper.apply {
                                translationY = 0F
                                visibility = View.GONE
                            }
                        }
                    })*/
                Animations.collapse(translationWrapper)
            }
        }
        ivDelete.setOnClickListener {
            exampleList.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
            notifyItemRangeChanged(adapterPosition, itemCount)
        }
    }

    companion object {
        const val EXAMPLE = 1
        const val TRANSLATION = 2

        @BindingAdapter("android:text")
        @JvmStatic
        fun setText(editText: EditText, spanned: Spanned) {
            if (editText.text != spanned) editText.setText(spanned)
        }

        @InverseBindingAdapter(attribute = "android:text")
        @JvmStatic
        fun getText(editText: EditText): Spanned {
            return editText.text
        }
    }
}