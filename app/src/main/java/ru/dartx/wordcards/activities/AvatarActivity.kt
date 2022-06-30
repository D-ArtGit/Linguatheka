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
import ru.dartx.wordcards.utils.BitmapManager
import java.io.FileNotFoundException
import java.io.IOException

class AvatarActivity : AppCompatActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar)
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.d("DArtX", "Res = ${it.resultCode}, Data = ${it.data}")
                if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                    val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = defPreference.edit()
                    try {
                        Toast.makeText(
                            this,
                            getString(R.string.avatar_applied),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        val stream =
                            contentResolver.openInputStream(it.data!!.data!!)
                        val realImage: Bitmap = BitmapFactory.decodeStream(stream)
                        editor.putString("avatar", BitmapManager.encodeToBase64(realImage))
                        editor.apply()
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                finish()
            }
        val i = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickImageLauncher.launch(i)
    }
}