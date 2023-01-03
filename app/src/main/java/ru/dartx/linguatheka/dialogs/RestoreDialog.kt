package ru.dartx.linguatheka.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import ru.dartx.linguatheka.databinding.RestoreDialogBinding

object RestoreDialog {
    fun showDialog(context: Context, listener: Listener, message1: String, message2: String) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        val binding = RestoreDialogBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        binding.apply {
            tvMessage1.text = message1
            tvMessage2.text = message2
            btOk.setOnClickListener {
                listener.onClickOk()
                dialog?.dismiss()
            }
            btCancel.setOnClickListener {
                listener.onClickCancel()
                dialog?.dismiss()
            }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    interface Listener {
        fun onClickOk()
        fun onClickCancel()

    }
}