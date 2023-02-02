package ru.dartx.linguatheka.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL

object GoogleSignInManager {
    fun googleSignIn(context: AppCompatActivity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestProfile()
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        return mGoogleSignInClient.signInIntent
    }

    fun googleSignOut(context: Context) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestProfile()
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        mGoogleSignInClient.signOut()
        val editor = defPreference.edit()
        editor.putString("user_name", "")
        editor.putString("avatar", "")
        editor.putBoolean("auto_backup", false)
        editor.apply()
    }

    fun setAvatar(context: Context, acc: GoogleSignInAccount, setUserName: Boolean) {
        val defPreference = PreferenceManager.getDefaultSharedPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            val editor = defPreference.edit()
            if (setUserName) editor.putString("user_name", acc.displayName)
            if (acc.photoUrl != null) {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = URL(acc.photoUrl!!.toString()).openStream()
                        val realImage: Bitmap = BitmapFactory.decodeStream(stream)
                        editor.putString(
                            "avatar",
                            BitmapManager.encodeToBase64(realImage)
                        )
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            editor.apply()
        }
    }
}