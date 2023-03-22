package ru.dartx.linguatheka.workers

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.activities.MainApp
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import java.util.*

class BackupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        createBackup()
        return Result.success()
    }

    private fun createBackup() {
        println("Start backup")
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
            println("Backup account is null")
        } else {
            val googleDriveService =
                BackupAndRestoreManager.googleDriveClient(account, applicationContext)
            if (googleDriveService != null) {
                if (BackupAndRestoreManager.isOnline(applicationContext)) {
                    backup(googleDriveService)
                } else {
                    println("There is no Internet connection")
                }
            } else {
                println("Google Drive Service is null")
            }
        }
    }

    private fun backup(googleDriveService: Drive) {
        val database = MainDataBase.getDataBase(applicationContext as MainApp)
        database.close()
        database.getDao().checkpoint((SimpleSQLiteQuery("pragma wal_checkpoint(full)")))
        MainDataBase.destroyInstance()

        val dbPath = applicationContext.getString(R.string.db_path)
        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = applicationContext.getString(R.string.file_name)
        val filePath = java.io.File(dbPath)
        val mediaContent = FileContent("", filePath)
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } catch (e: GoogleJsonResponseException) {
                println("Unable upload: " + e.details)
                throw e
            }
        }
    }
}