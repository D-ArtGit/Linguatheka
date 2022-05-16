package ru.dartx.wordcards.utils

import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log

object HtmlManager {
    fun getFromHtml(text: String): Spanned {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
    }

    fun toHtml(text: Spanned): String {
        return Html.toHtml(text, Html.FROM_HTML_MODE_COMPACT)
    }
}