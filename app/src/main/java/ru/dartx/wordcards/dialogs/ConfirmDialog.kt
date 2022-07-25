package ru.dartx.wordcards.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ConfirmDialogBinding


object ConfirmDialog {
    fun showDialog(context: Context, listener: Listener, message: String) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        val binding = ConfirmDialogBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        binding.apply {
            tvMessage.text = message
            btOk.setOnClickListener {
                listener.onClick()
                dialog?.dismiss()
            }
            btCancel.setOnClickListener { dialog?.dismiss() }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    interface Listener {
        fun onClick()
    }
}