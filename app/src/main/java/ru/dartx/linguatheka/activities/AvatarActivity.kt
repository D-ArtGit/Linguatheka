package ru.dartx.linguatheka.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.dialogs.AvatarDialog
import ru.dartx.linguatheka.utils.BitmapManager
import ru.dartx.linguatheka.utils.GoogleSignInManager
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

        val singInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val acc = GoogleSignIn.getLastSignedInAccount(this)
                if (acc != null) {
                    GoogleSignInManager.setAvatar(this, acc, true)
                    Toast.makeText(
                        this,
                        getString(R.string.avatar_applied),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else GoogleSignInManager.googleSignOut(this)
            } else {
                Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                    .show()
            }
            finish()
        }


        val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
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
            }
        AvatarDialog.showDialog(this, object : AvatarDialog.Listener {
            override fun onClickChoose() {
                pickImageLauncher
                    .launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun onClickClear() {
                editor.putString("avatar", "")
                editor.apply()
                finish()
            }

            override fun onClickCancel() {
                finish()
            }

            override fun onClickGoogle() {
                val acc = GoogleSignIn.getLastSignedInAccount(this@AvatarActivity)
                if (acc == null) singInLauncher.launch(
                    GoogleSignInManager.googleSignIn(
                        this@AvatarActivity
                    )
                )
                else {
                    GoogleSignInManager.setAvatar(this@AvatarActivity, acc, false)
                    Toast.makeText(
                        this@AvatarActivity,
                        getString(R.string.avatar_applied),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    finish()
                }
            }
        })
    }
}