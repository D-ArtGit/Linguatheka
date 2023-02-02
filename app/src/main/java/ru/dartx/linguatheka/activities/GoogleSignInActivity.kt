package ru.dartx.linguatheka.activities

import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.dialogs.ConfirmDialog
import ru.dartx.linguatheka.utils.GoogleSignInManager
import ru.dartx.linguatheka.utils.ThemeManager

class GoogleSignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
                } else GoogleSignInManager.googleSignOut(this)
            } else {
                Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                    .show()
            }
            finish()
        }
        val message = getString(R.string.confirm_grant_access)
        ConfirmDialog.showDialog(this, object : ConfirmDialog.Listener {
            override fun onClick() {
                singInLauncher.launch(GoogleSignInManager.googleSignIn(this@GoogleSignInActivity))
            }
            override fun onCancel() {
                finish()
            }
        }, message)
    }
}