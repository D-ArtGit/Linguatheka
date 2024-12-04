package ru.dartx.linguatheka.presentation.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.presentation.dialogs.AvatarDialog
import ru.dartx.linguatheka.utils.BitmapManager
import ru.dartx.linguatheka.utils.ThemeManager
import java.io.FileNotFoundException
import java.io.IOException

class AvatarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = defPreference.edit()
        setTheme(ThemeManager.getSelectedDialogTheme(this))
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_avatar)

        val pickImageLauncher =
            registerForActivityResult(CropImageContract()) {
                if (it?.uriContent != null) {
                    try {
                        val stream =
                            contentResolver.openInputStream(it.uriContent!!)
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
        /*registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            if (it != null) {
                try {
                    val stream =
                        contentResolver.openInputStream(it)
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
        }*/
        AvatarDialog.showDialog(this, object : AvatarDialog.Listener {
            override fun onClickChoose() {
                pickImageLauncher.launch(
                    CropImageContractOptions(
                        uri = null,
                        cropImageOptions = CropImageOptions(
                            imageSourceIncludeCamera = true,
                            imageSourceIncludeGallery = true,
                            fixAspectRatio = true,
                            activityMenuIconColor = Color.BLACK
                        )
                    )
                )
                //.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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