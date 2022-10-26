package ru.dartx.wordcards.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dartx.wordcards.R
import ru.dartx.wordcards.databinding.ActivityBackupBinding
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import java.util.*

class BackupActivity : AppCompatActivity() {
    lateinit var binding: ActivityBackupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("DArtX", "Start Backup")
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Log.d("DArtX", "Backup account null")
            Toast.makeText(this, getString(R.string.login_to_google), Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d("DArtX", "Backup account: ${account.account}")
            val googleDriveService =
                BackupAndRestoreManager.googleDriveClient(account, this)
            if (googleDriveService != null && BackupAndRestoreManager.isOnline(this)) {
                binding.pbLoading.visibility = View.VISIBLE
                backup(googleDriveService)
            } else {
                when (BackupAndRestoreManager.isOnline(this)) {
                    true -> Toast.makeText(
                        this,
                        getString(R.string.relogin_to_google),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    false -> Toast.makeText(
                        this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish()
            }
        }
    }

    private fun backup(googleDriveService: Drive) {
        Log.d("DArtX", "Upload")

        Toast.makeText(this, getString(R.string.backup_started), Toast.LENGTH_SHORT).show()
        MainDataBase.destroyInstance()
        val dbPath = getString(R.string.db_path)

        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = getString(R.string.file_name)

        val filePath = java.io.File(dbPath)
        val mediaContent = FileContent("", filePath)
        CoroutineScope(Dispatchers.IO).launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    Log.d("DArtX", "Try upload")
                    val uploadedFiles = googleDriveService.files().list()
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
                    Log.d("DArtX", "Try e1")
                    println("Unable upload: " + e.details)
                    throw e
                }
            }
            withContext(Dispatchers.Main) {
                Log.d("DArtX", success.toString())
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