package ru.dartx.linguatheka.presentation.adapters

import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.*
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ExampleItemEditBinding
import ru.dartx.linguatheka.databinding.ExampleItemViewBinding
import ru.dartx.linguatheka.domain.ExampleItemUiState
import ru.dartx.linguatheka.utils.Animations

class ExampleAdapter :
    ListAdapter<ExampleItemUiState, ExampleAdapter.ExampleViewHolder>(ExampleDiffCallback()) {

    var onDeleteClickListener: ((Int) -> Unit)? = null
    var onRequestFocusWithKeyboard: ((EditText, Int) -> Unit)? = null
    var onTextChangedAfterError: ((Int) -> Unit)? = null

    inner class ExampleViewHolder(val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).editMode) EDIT_MODE else VIEW_MODE

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
        val layout = when (viewType) {
            EDIT_MODE -> R.layout.example_item_edit
            VIEW_MODE -> R.layout.example_item_view
            else -> throw RuntimeException("Unknown layout")
        }
        return ExampleViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                layout,
                parent,
                false
            )
        ).apply { onViewHolderCreated(this, viewType, binding) }
    }

    override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
        val binding = holder.binding
        val exampleItem = getItem(position)

        with(binding) {
            when (this) {
                is ExampleItemEditBinding -> {
                    launchEditMode(this, exampleItem)
                }

                is ExampleItemViewBinding -> {
                    launchViewMode(this, exampleItem)
                }
            }
        }
    }

    private fun launchViewMode(
        exampleItemViewBinding: ExampleItemViewBinding,
        exampleItemUiState: ExampleItemUiState
    ) {
        with(exampleItemViewBinding) {
            this.exampleItemUiState = exampleItemUiState
        }
    }

    private fun launchEditMode(
        exampleItemEditBinding: ExampleItemEditBinding,
        exampleItemUiState: ExampleItemUiState
    ) {
        with(exampleItemEditBinding) {
            this.exampleItemUiState = exampleItemUiState
            tvExampleHeader.text =
                tvExampleHeader.context.getString(R.string.example, exampleItemUiState.itemNumber)
            if (exampleItemUiState.requestFocus) {
                onRequestFocusWithKeyboard?.invoke(edExample, exampleItemUiState.id)
            }
        }
    }

    private fun onViewHolderCreated(
        viewHolder: ExampleViewHolder,
        viewType: Int,
        binding: ViewDataBinding
    ) {
        when (viewType) {
            EDIT_MODE -> setOnEditModeListeners(
                binding as ExampleItemEditBinding,
                viewHolder
            )

            VIEW_MODE -> setOnViewModeListeners(
                binding as ExampleItemViewBinding
            )
        }
    }

    private fun setOnViewModeListeners(
        binding: ExampleItemViewBinding
    ) {
        with(binding) {
            tvExample.setOnClickListener {
                showHide(binding)
            }
            ivShowHide.setOnClickListener {
                showHide(binding)
            }
        }
    }

    private fun showHide(binding: ExampleItemViewBinding) {
        with(binding) {
            if (translationWrapper.visibility == View.GONE) {
                ivShowHide.animate().rotation(180F).start()
                Animations.expand(translationWrapper, tvExample.width)
            } else {
                ivShowHide.animate().rotation(0F).start()
                Animations.collapse(translationWrapper)
            }
        }
    }

    private fun setOnEditModeListeners(
        binding: ExampleItemEditBinding,
        viewHolder: ExampleViewHolder
    ) = with(binding) {
        edExample.customSelectionActionModeCallback =
            getActionModeCallback(binding, EXAMPLE)
        edTranslation.customSelectionActionModeCallback = getActionModeCallback(
            binding,
            TRANSLATION
        )

        edExample.addTextChangedListener { text ->
            if (!text.isNullOrBlank() &&
                getItem(viewHolder.adapterPosition).error != null
            ) {
                onTextChangedAfterError?.invoke(getItem(viewHolder.adapterPosition).id)
            }
        }

        ivDelete.setOnClickListener {
            onDeleteClickListener?.invoke(getItem(viewHolder.adapterPosition).id)
            notifyItemRemoved(viewHolder.adapterPosition)
            notifyItemRangeChanged(viewHolder.adapterPosition, currentList.size)
        }
    }

    private fun getActionModeCallback(
        binding: ExampleItemEditBinding,
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
        binding: ExampleItemEditBinding,
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
                exampleItemUiState?.example = edExample.text!!
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
                exampleItemUiState?.translation = edTranslation.text!!
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

    companion object {
        const val EXAMPLE = 1
        const val TRANSLATION = 2
        const val EDIT_MODE = 1
        const val VIEW_MODE = 0

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