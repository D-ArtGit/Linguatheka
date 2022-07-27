package ru.dartx.wordcards.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import ru.dartx.wordcards.databinding.AvatarDialogBinding

object AvatarDialog {
    fun showDialog(context: Context, listener: Listener) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        val binding = AvatarDialogBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        binding.apply {
            btChoose.setOnClickListener {
                listener.onClickChoose()
                dialog?.dismiss()
            }
            btClear.setOnClickListener {
                listener.onClickClear()
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
        fun onClickChoose()
        fun onClickClear()
        fun onClickCancel()

    }
}