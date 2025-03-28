package ru.dartx.linguatheka.presentation.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import ru.dartx.linguatheka.databinding.AboutAppDialogBinding
import ru.dartx.linguatheka.presentation.activities.LargeTextActivity

object AboutAppDialog {
    fun showDialog(context: Context, message: String) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        val binding = AboutAppDialogBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        binding.apply {
            tvVersion.text = message
            var spannable = SpannableString(tvPrivacy.text)
            spannable.setSpan(URLSpan(""), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvPrivacy.text = spannable
            spannable = SpannableString(tvAgreement.text)
            spannable.setSpan(URLSpan(""), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvAgreement.text = spannable
            btClose.setOnClickListener {
                dialog?.dismiss()
            }
            tvPrivacy.setOnClickListener {
                val i = LargeTextActivity.intentForPrivacy(context)
                context.startActivity(i)
            }
            tvAgreement.setOnClickListener {
                val i = LargeTextActivity.intentForAgreement(context)
                context.startActivity(i)
            }
        }
        dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            it.dismiss()
        }
        dialog.show()
    }

}