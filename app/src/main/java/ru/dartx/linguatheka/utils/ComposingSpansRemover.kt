package ru.dartx.linguatheka.utils

import android.text.Spannable
import android.text.Spanned

object ComposingSpansRemover {
    fun removeComposingSpans(text: Spanned): Spanned {
        val tempText = text as Spannable
        val sps = tempText.getSpans(0, tempText.length, Object::class.java)
        if (sps != null) {
            for (i in sps.indices) {
                val o = sps[i]
                if ((tempText.getSpanFlags(o) and Spanned.SPAN_COMPOSING) != 0) tempText.removeSpan(o)
            }
        }
        return tempText
    }
}