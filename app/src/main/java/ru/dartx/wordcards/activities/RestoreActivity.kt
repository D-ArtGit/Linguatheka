package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.Drive
import kotlinx.coroutines.*
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityRestoreBinding
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.dialogs.RestoreDialog
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import java.io.FileOutputStream
import java.io.IOException

class RestoreActivity : AppCompatActivity() {
    lateinit var binding: ActivityRestoreBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("DArtX", "Start Restore")
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Log.d("DArtX", "Restore account null")
            Toast.makeText(this, getString(R.string.login_to_google), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Log.d("DArtX", "Restore account: ${account.account}")
            val message1 = getString(R.string.restore_message1)
            val message2 = getString(R.string.restore_message2)
            RestoreDialog.showDialog(this, object : RestoreDialog.Listener {
                override fun onClickOk() {
                    Log.d("DArtX", "Restore Click")
                    val googleDriveService =
                        BackupAndRestoreManager.googleDriveClient(account, this@RestoreActivity)
                    if (googleDriveService != null
                        && BackupAndRestoreManager.isOnline(this@RestoreActivity)
                    ) {
                        Toast.makeText(
                            this@RestoreActivity,
                            getString(R.string.restore_started),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.pbLoading.visibility = View.VISIBLE
                        CoroutineScope(Dispatchers.Main).launch { restore(googleDriveService) }
                    } else {
                        when (BackupAndRestoreManager.isOnline(this@RestoreActivity)) {
                            true -> Toast.makeText(
                                this@RestoreActivity,
                                getString(R.string.relogin_to_google),
                                Toast.LENGTH_SHORT
                            ).show()
                            false -> Toast.makeText(
                                this@RestoreActivity,
                                getString(R.string.no_internet),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        finish()
                    }
                }

                override fun onClickCancel() {
                    finish()
                }
            }, message1, message2)
        }
    }

    private suspend fun restore(googleDriveService: Drive) {
        Log.d("DArtX", "Restore started")
        var restoreSuccess = false
        val success = withContext(Dispatchers.IO) {
            val files = googleDriveService.files().list()
                .setSpaces("appDataFolder")
                .setPageSize(10)
                .execute()
            if (files.files.size == 1) {
                Log.d("DArtX", "Files exist: ${files.files.size}")
                MainDataBase.destroyInstance()
                try {
                    val dir = java.io.File(getString(R.string.path))
                    if (dir.isDirectory) {
                        val children = dir.list()
                        if (children != null) {
                            for (i in children.indices) {
                                println(
                                    "File ${
                                        java.io.File(
                                            dir,
                                            children[i]
                                        ).name
                                    } deleted from db"
                                )
                                java.io.File(dir, children[i]).delete()
                            }
                        }
                    }
                    for (file in files.files) {
                        when (file.name) {
                            getString(R.string.file_name) -> {
                                val outputStream = FileOutputStream(getString(R.string.db_path))
                                googleDriveService.files().get(file.id)
                                    .executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                                println("File restored: " + file.name + " " + file.id)
                            }
                        }
                    }
                    restoreSuccess = true
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            restoreSuccess
        }
        withContext(Dispatchers.Main) {
            Log.d("DArtX", success.toString())
            if (success) {
                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restored_success),
                    Toast.LENGTH_LONG
                ).show()
                val i = Intent(this@RestoreActivity, MainActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            } else {
                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restore_failed),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
}