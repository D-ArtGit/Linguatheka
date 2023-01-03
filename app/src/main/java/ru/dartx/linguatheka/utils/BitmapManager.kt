package ru.dartx.linguatheka.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.util.*

object BitmapManager {
    fun encodeToBase64(image: Bitmap): String {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    fun decodeToBase64(input: String): Bitmap {
        val decodedByte = Base64.getDecoder().decode(input)
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
    }
}