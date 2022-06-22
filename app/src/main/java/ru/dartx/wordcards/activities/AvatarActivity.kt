package ru.dartx.wordcards.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.R
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class AvatarActivity : AppCompatActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar)
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                    val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = defPreference.edit()
                    try {
                        Log.d("DArtX", "Pick")
                        Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
                        val stream =
                            contentResolver.openInputStream(it.data!!.data!!)
                        val realImage: Bitmap = BitmapFactory.decodeStream(stream)
                        editor.putString("avatar", encodeToBase64(realImage))
                        editor.apply()
                    } catch (e: FileNotFoundException) {
                        Log.d("DArtX", "Pick E1")
                        e.printStackTrace()
                        editor.putString("avatar", "")
                        editor.apply()
                    } catch (e: IOException) {
                        Log.d("DArtX", "Pick E2")
                        e.printStackTrace()
                        editor.putString("avatar", "")
                        editor.apply()
                    }
                    finish()
                }
            }
        val i = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickImageLauncher.launch(i)
    }

    private fun encodeToBase64(image: Bitmap): String {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }
}