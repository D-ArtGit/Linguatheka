package ru.dartx.linguatheka.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import ru.dartx.linguatheka.databinding.AvatarDialogBinding
import ru.dartx.linguatheka.utils.BackupAndRestoreManager

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
            if (BackupAndRestoreManager.checkForGooglePlayServices(context)) {
                btGoogle.setOnClickListener {
                    listener.onClickGoogle()
                    dialog?.dismiss()
                }
            } else btGoogle.visibility = View.GONE
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            listener.onClickCancel()
            it.dismiss()
        }
        dialog.show()
        dialog.setOnKeyListener { dialogInListener, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                listener.onClickCancel()
                dialogInListener.dismiss()
            }
            true
        }
    }

    interface Listener {
        fun onClickChoose()
        fun onClickClear()
        fun onClickCancel()
        fun onClickGoogle()

    }
}