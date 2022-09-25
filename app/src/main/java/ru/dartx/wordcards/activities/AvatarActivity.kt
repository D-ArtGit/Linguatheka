package ru.dartx.wordcards.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ru.dartx.wordcards.R
import ru.dartx.wordcards.dialogs.AvatarDialog
import ru.dartx.wordcards.utils.BitmapManager
import java.io.FileNotFoundException
import java.io.IOException

class AvatarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = defPreference.edit()
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_avatar)
        val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                    try {
                        val stream =
                            contentResolver.openInputStream(it.data!!.data!!)
                        val realImage: Bitmap = BitmapFactory.decodeStream(stream)
                        editor.putString("avatar", BitmapManager.encodeToBase64(realImage))
                        editor.apply()
                        Toast.makeText(
                            this,
                            getString(R.string.avatar_applied),
                            Toast.LENGTH_SHORT
                        )
                            .show()
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
        AvatarDialog.showDialog(this, object : AvatarDialog.Listener {
            override fun onClickChoose() {
                pickImageLauncher.launch(i)
            }

            override fun onClickClear() {
                editor.putString("avatar", "")
                editor.apply()
                finish()
            }

            override fun onClickCancel() {
                finish()
            }
        })
    }
}