package ru.dartx.linguatheka.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityRestoreBinding
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.presentation.dialogs.RestoreDialog
import ru.dartx.linguatheka.utils.AuthorizationClientManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager.isGrantedAllScopes
import ru.dartx.linguatheka.utils.ThemeManager
import java.io.FileOutputStream
import java.io.IOException

class RestoreActivity : AppCompatActivity() {
    lateinit var binding: ActivityRestoreBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getSelectedDialogTheme(this))
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authorizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(it.data)
                if (isGrantedAllScopes(authorizationResult)) {
                    confirmRestore(authorizationResult)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.grant_access),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    finish()
                }
            } else {
                Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                    .show()

            }
        }

        AuthorizationClientManager.authorize(
            this,
            authorizationLauncher
        ) { authorizationResult ->
            if (isGrantedAllScopes(authorizationResult)) {
                confirmRestore(authorizationResult)
            } else {
                Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    private fun confirmRestore(authorizationResult: AuthorizationResult) {
        val message1 = getString(R.string.restore_message1)
        val message2 = getString(R.string.restore_message2)
        RestoreDialog.showDialog(this, object : RestoreDialog.Listener {
            override fun onClickOk() {
                if (!BackupAndRestoreManager.isOnline(this@RestoreActivity)) {
                    Toast.makeText(
                        this@RestoreActivity,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                val driveService =
                    BackupAndRestoreManager.googleDriveClient(
                        authorizationResult,
                        this@RestoreActivity
                    )

                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restore_started),
                    Toast.LENGTH_SHORT
                ).show()
                binding.pbLoading.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Main).launch { restore(driveService) }
            }

            override fun onClickCancel() {
                finish()
            }
        }, message1, message2)
    }

    private suspend fun restore(driveService: Drive) {
        var restoreSuccess = false
        val success = withContext(Dispatchers.IO) {
            try {
                val files = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setPageSize(10)
                    .execute()
                if (files.files.size == 1) {
                    MainDataBase.destroyInstance()
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
                                driveService.files().get(file.id)
                                    .executeMediaAndDownloadTo(outputStream)
                                outputStream.close()
                                println("File restored: " + file.name + " " + file.id)
                            }
                        }
                    }
                    restoreSuccess = true

                }
            } catch (e: IOException) {
                e.printStackTrace()
                restoreSuccess = false
            } catch (e: Exception) {
                e.printStackTrace()
                restoreSuccess = false
            }
            restoreSuccess
        }
        withContext(Dispatchers.Main) {
            if (success) {
                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restored_success),
                    Toast.LENGTH_LONG
                ).show()
                val i = Intent(this@RestoreActivity, MainActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            } else {
                Toast.makeText(
                    this@RestoreActivity,
                    getString(R.string.restore_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }
    }
}