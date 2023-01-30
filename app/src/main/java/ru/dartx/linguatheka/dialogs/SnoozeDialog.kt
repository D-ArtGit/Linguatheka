package ru.dartx.linguatheka.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.SnoozeDialogBinding

object SnoozeDialog {
    fun showSnoozeDialog(context: Context, listener: Listener) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        var snoozeTime = defPreference.getString("snooze", "2")!!.toInt()
        var seekBarText =
            context.resources.getQuantityString(R.plurals.hours, snoozeTime, snoozeTime)
        val binding = SnoozeDialogBinding.inflate(LayoutInflater.from(context))
        binding.tvSeekBarValue.text = seekBarText
        val seekBar = binding.seekBar
        seekBar.progress = when (snoozeTime) {
            1 -> 0
            4 -> 2
            8 -> 3
            else -> 1
        }
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                snoozeTime = when (progress) {
                    0 -> 1
                    2 -> 4
                    3 -> 8
                    else -> 2
                }
                seekBarText =
                    context.resources.getQuantityString(R.plurals.hours, snoozeTime, snoozeTime)
                binding.tvSeekBarValue.text = seekBarText
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        builder.setView(binding.root)
        binding.apply {
            btOk.setOnClickListener {
                listener.onOkClick(snoozeTime)
                dialog?.dismiss()
            }
            btCancel.setOnClickListener {
                listener.onCancelClick()
                dialog?.dismiss()
            }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            listener.onCancelClick()
            it.dismiss()
        }
        dialog.show()
        dialog.setOnKeyListener { dialogInListener, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                listener.onCancelClick()
                dialogInListener.dismiss()
            }
            true
        }
    }

    interface Listener {
        fun onOkClick(delay: Int)
        fun onCancelClick()
    }
}