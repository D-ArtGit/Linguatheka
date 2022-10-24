package ru.dartx.wordcards.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dartx.wordcards.R
import ru.dartx.wordcards.db.MainDataBase
import ru.dartx.wordcards.utils.BackupAndRestoreManager
import java.util.*

class BackupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val bwContext = context
    override fun doWork(): Result {
        createBackup()
        return Result.success()
    }

    private fun createBackup() {
        Log.d("DArtX", "Start Backup from Worker")
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account == null) {
            println("Backup account is null")
        } else {
            Log.d("DArtX", "Backup account in worker: ${account.account}")
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
        val database = MainDataBase.getDataBase(bwContext)
        database.close()
        val dbPath = applicationContext.getString(R.string.db_path)
        val dbPathShm = applicationContext.getString(R.string.db_path_shm)
        val dbPathWal = applicationContext.getString(R.string.db_path_wal)

        val storageFile = com.google.api.services.drive.model.File()
        storageFile.parents = Collections.singletonList("appDataFolder")
        storageFile.name = applicationContext.getString(R.string.file_name)
        val storageFileShm = com.google.api.services.drive.model.File()
        storageFileShm.parents = Collections.singletonList("appDataFolder")
        storageFileShm.name = applicationContext.getString(R.string.file_shm_name)
        val storageFileWal = com.google.api.services.drive.model.File()
        storageFileWal.parents = Collections.singletonList("appDataFolder")
        storageFileWal.name = applicationContext.getString(R.string.file_wal_name)

        val filePath = java.io.File(dbPath)
        val filePathShm = java.io.File(dbPathShm)
        val filePathWal = java.io.File(dbPathWal)
        val mediaContent = FileContent("", filePath)
        val mediaContentShm = FileContent("", filePathShm)
        val mediaContentWal = FileContent("", filePathWal)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DArtX", "Try upload from worker")
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
                        if (uploadedFile.id != file.id
                            && uploadedFile.name == applicationContext.getString(
                                R.string.file_name
                            )
                        ) {
                            googleDriveService.files().delete(uploadedFile.id).execute()
                            println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                        }
                    }
                }
                val fileShm =
                    googleDriveService.files().create(storageFileShm, mediaContentShm)
                        .setFields("id")
                        .execute()
                println("Filename: " + fileShm.id)
                if (!file.id.isNullOrEmpty()) {
                    for (uploadedFile in uploadedFiles.files) {
                        if (uploadedFile.id != fileShm.id
                            && uploadedFile.name == applicationContext.getString(R.string.file_shm_name)
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
                            && uploadedFile.name == applicationContext.getString(R.string.file_wal_name)
                        ) {
                            googleDriveService.files().delete(uploadedFile.id).execute()
                            println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                        }
                    }
                }
            } catch (e: GoogleJsonResponseException) {
                Log.d("DArtX", "Try e1")
                println("Unable upload: " + e.details)
                throw e
            }
        }
    }
}