package ru.dartx.linguatheka.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityBackupBinding
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.utils.GoogleSignInManager
import ru.dartx.linguatheka.utils.ThemeManager
import java.util.*

class BackupActivity : AppCompatActivity() {
    lateinit var binding: ActivityBackupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedDialogTheme(this))
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!BackupAndRestoreManager.isOnline(this)) {
            Toast.makeText(
                this,
                getString(R.string.no_internet),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            val singInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == RESULT_OK) {
                    val acc = GoogleSignIn.getLastSignedInAccount(this)
                    if (acc != null) {
                        GoogleSignInManager.setAvatar(this, acc, true)
                        backup(acc)
                    } else {
                        GoogleSignInManager.googleSignOut(this)
                        Toast.makeText(this, getString(R.string.try_later), Toast.LENGTH_LONG)
                            .show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
            singInLauncher.launch(GoogleSignInManager.googleSignIn(this))
        } else backup(account)
    }

    private fun backup(account: GoogleSignInAccount) {
        val googleDriveService =
            BackupAndRestoreManager.googleDriveClient(account, this)
        if (googleDriveService == null) {
            Toast.makeText(
                this,
                getString(R.string.try_later),
                Toast.LENGTH_LONG
            )
                .show()
            finish()
        }

        binding.pbLoading.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.backup_started), Toast.LENGTH_SHORT).show()

        MainDataBase.destroyInstance()
        val dbPath = getString(R.string.db_path)
        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = getString(R.string.file_name)

        val filePath = java.io.File(dbPath)
        val mediaContent = FileContent("", filePath)
        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val uploadedFiles = googleDriveService!!.files().list()
                        .setSpaces("appDataFolder")
                        .setPageSize(10)
                        .execute()
                    val file =
                        googleDriveService.files().create(storageFile, mediaContent)
                            .setFields("id")
                            .execute()
                    println("Filename: " + file.id)
                    if (!file.id.isNullOrEmpty()) {
                        for (uploadedFile in uploadedFiles.files) {
                            if (uploadedFile.id != file.id) {
                                googleDriveService.files().delete(uploadedFile.id).execute()
                                println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                            }
                        }
                    }
                    true
                } catch (e: GoogleJsonResponseException) {
                    println("Unable upload: " + e.details)
                    false
                }
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@BackupActivity,
                        getString(R.string.backup_success),
                        Toast.LENGTH_LONG
                    ).show()
                    val i = Intent(this@BackupActivity, MainActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                } else {
                    Toast.makeText(
                        this@BackupActivity,
                        getString(R.string.backup_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        }
    }
}