package ru.dartx.wordcards.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.DelayDialogBinding

object DelayDialog {
    fun showDelayDialog(context: Context, listener: Listener) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        var delayTime = 2
        var seekBarText = context.resources.getQuantityString(R.plurals.hours, delayTime, delayTime)
        val binding = DelayDialogBinding.inflate(LayoutInflater.from(context))
        binding.tvSeekBarValue.text = seekBarText
        val seekBar = binding.seekBar
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayTime = when (progress) {
                    0 -> 1
                    2 -> 4
                    3 -> 8
                    else -> 2
                }
                seekBarText =
                    context.resources.getQuantityString(R.plurals.hours, delayTime, delayTime)
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
                listener.onOkClick(delayTime)
                dialog?.dismiss()
            }
            btCancel.setOnClickListener {
                listener.onCancelClick()
                dialog?.dismiss()
            }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    interface Listener {
        fun onOkClick(delay: Int)
        fun onCancelClick()
    }
}