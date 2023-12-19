package ru.dartx.linguatheka.presentation.viewmodels

interface OnActionListener {
    fun onFinished(message: String, isChecked: Boolean)
    fun setState(state: Int)

    companion object{
        const val CARD_STATE_NEW = 1
        const val CARD_STATE_EDIT = 2
        const val CARD_STATE_VIEW = 3
        const val CARD_STATE_CHECK = 4
    }
}