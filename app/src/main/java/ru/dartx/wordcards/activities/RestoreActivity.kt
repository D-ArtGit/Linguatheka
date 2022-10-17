package ru.dartx.wordcards.activities

import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.wordcards.R
import ru.dartx.wordcards.dialogs.RestoreDialog
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import java.io.FileOutputStream
import java.io.IOException

class RestoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_restore)
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
                        CoroutineScope(Dispatchers.Main).launch { restore(googleDriveService) }
                        //delete(googleDriveService)
                        finish()
                    } else {
                        Toast.makeText(
                            this@RestoreActivity,
                            getString(R.string.no_internet),
                            Toast.LENGTH_LONG
                        ).show()
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
            if (files.files.size == 3) {
                Log.d("DArtX", "Files exist: ${files.files.size}")
                try {
                    val dir = java.io.File(getString(R.string.path))
                    if (dir.isDirectory) {
                        val children = dir.list()
                        if (children != null) {
                            for (i in children.indices) {
                                println("File ${java.io.File(dir, children[i]).name} deleted")
                                java.io.File(dir, children[i]).delete()
                            }
                        }
                    }
                    for (file in files.files) {
                        println("File restored: " + file.name + " " + file.id)
                        when (file.name) {
                            getString(R.string.file_name) -> {
                                val outputStream = FileOutputStream(getString(R.string.db_path))
                                googleDriveService.files().get(file.id)
                                    .executeMediaAndDownloadTo(outputStream)
                            }
                            getString(R.string.file_shm_name) -> {
                                val outputStream = FileOutputStream(getString(R.string.db_path_shm))
                                googleDriveService.files().get(file.id)
                                    .executeMediaAndDownloadTo(outputStream)
                            }
                            getString(R.string.file_wal_name) -> {
                                val outputStream = FileOutputStream(getString(R.string.db_path_wal))
                                googleDriveService.files().get(file.id)
                                    .executeMediaAndDownloadTo(outputStream)
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
            } else {
                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restore_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun delete(googleDriveService: Drive) {
        Log.d("DArtX", "Delete started")
        CoroutineScope(Dispatchers.IO).launch {
            val files = googleDriveService.files().list()
                .setSpaces("appDataFolder")
                .setPageSize(10)
                .execute()
            if (files.files.size != 0) {
                Log.d("DArtX", "Files exist")
                try {
                    for (file in files.files) {
                        println("File deleted: " + file.name + " " + file.id)
                        googleDriveService.files().delete(file.id).execute()
                    }
                    googleDriveService.files().emptyTrash().execute()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            Log.d("DArtX", "After delete")
        }
    }
}