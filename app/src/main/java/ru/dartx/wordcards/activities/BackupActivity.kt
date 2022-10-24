package ru.dartx.wordcards.activities

import android.os.Bundle
import android.util.Log
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
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import java.util.*

class BackupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_backup)
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
            if (googleDriveService != null) {
                backup(googleDriveService)
                //delete(googleDriveService)
                finish()
            }
        }
    }

    private fun backup(googleDriveService: Drive) {
        Log.d("DArtX", "Upload")
        if (BackupAndRestoreManager.isOnline(this)) {
            Toast.makeText(this, getString(R.string.backup_started), Toast.LENGTH_SHORT).show()
            MainDataBase.destroyInstance()
            val dbPath = getString(R.string.db_path)
            val dbPathShm = getString(R.string.db_path_shm)
            val dbPathWal = getString(R.string.db_path_wal)

            val storageFile = com.google.api.services.drive.model.File()
            storageFile.parents = Collections.singletonList("appDataFolder")
            storageFile.name = getString(R.string.file_name)
            val storageFileShm = com.google.api.services.drive.model.File()
            storageFileShm.parents = Collections.singletonList("appDataFolder")
            storageFileShm.name = getString(R.string.file_shm_name)
            val storageFileWal = com.google.api.services.drive.model.File()
            storageFileWal.parents = Collections.singletonList("appDataFolder")
            storageFileWal.name = getString(R.string.file_wal_name)

            val filePath = java.io.File(dbPath)
            val filePathShm = java.io.File(dbPathShm)
            val filePathWal = java.io.File(dbPathWal)
            val mediaContent = FileContent("", filePath)
            val mediaContentShm = FileContent("", filePathShm)
            val mediaContentWal = FileContent("", filePathWal)
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
                                if (uploadedFile.id != file.id /*&& uploadedFile.name == getString(R.string.file_name)*/) {
                                    googleDriveService.files().delete(uploadedFile.id).execute()
                                    println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                                }
                            }
                        }
                        /*val fileShm =
                            googleDriveService.files().create(storageFileShm, mediaContentShm)
                                .setFields("id")
                                .execute()
                        println("Filename: " + fileShm.id)
                        if (!file.id.isNullOrEmpty()) {
                            for (uploadedFile in uploadedFiles.files) {
                                if (uploadedFile.id != fileShm.id
                                    && uploadedFile.name == getString(R.string.file_shm_name)
                                ) {
                                    googleDriveService.files().delete(uploadedFile.id).execute()
                                    println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                                }
                            }
                        }
                        val fileWal =
                            googleDriveService.files().create(storageFileWal, mediaContentWal)
                                .setFields("id")
                                .execute()
                        println("Filename: " + fileWal.id)
                        if (!file.id.isNullOrEmpty()) {
                            for (uploadedFile in uploadedFiles.files) {
                                if (uploadedFile.id != fileWal.id
                                    && uploadedFile.name == getString(R.string.file_wal_name)
                                ) {
                                    googleDriveService.files().delete(uploadedFile.id).execute()
                                    println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                                }
                            }
                        }*/
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
                    } else {
                        Toast.makeText(
                            this@BackupActivity,
                            getString(R.string.backup_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show()
        }
    }
}