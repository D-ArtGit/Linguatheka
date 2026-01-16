package ru.dartx.linguatheka.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.databinding.ActivityBackupBinding
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.utils.AuthorizationClientManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager.isGrantedAllScopes
import ru.dartx.linguatheka.utils.ThemeManager
import java.util.Collections

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
            return
        }
        val singInLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val authorizationResult = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(it.data)
                if (isGrantedAllScopes(authorizationResult)) {
                    backup(authorizationResult)
                } else {
                    Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }
        AuthorizationClientManager.authorize(this, singInLauncher) { authorizationResult ->
            if (isGrantedAllScopes(authorizationResult)) {
                backup(authorizationResult)
            } else {
                Toast.makeText(this, getString(R.string.grant_access), Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    private fun backup(authorizationResult: AuthorizationResult) {
        val driveService =
            BackupAndRestoreManager.googleDriveClient(authorizationResult, this)
        binding.pbLoading.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.backup_started), Toast.LENGTH_SHORT).show()
        val dbPath = getString(R.string.db_path)
        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = getString(R.string.file_name)

        val filePath = java.io.File(dbPath)
        val mediaContent = FileContent("", filePath)
        CoroutineScope(Dispatchers.Main).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val database = MainDataBase.getDataBase(applicationContext as MainApp)
                    database.getDao().checkpoint((SimpleSQLiteQuery("pragma wal_checkpoint(full)")))
                    MainDataBase.destroyInstance()
                    val uploadedFiles = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setPageSize(10)
                        .execute()
                    val file =
                        driveService.files().create(storageFile, mediaContent)
                            .setFields("id")
                            .execute()
                    println("Filename: " + file.id)
                    if (!file.id.isNullOrEmpty()) {
                        for (uploadedFile in uploadedFiles.files) {
                            if (uploadedFile.id != file.id) {
                                driveService.files().delete(uploadedFile.id).execute()
                                println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                            }
                        }
                    }
                    true
                } catch (e: GoogleJsonResponseException) {
                    println("Unable upload: ${e.details}")
                    false
                } catch (e: Exception) {
                    println("Unable upload: ${e.message}")
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