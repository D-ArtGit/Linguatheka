package ru.dartx.linguatheka.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import ru.dartx.linguatheka.R

object BackupAndRestoreManager {
    fun googleDriveClient(authorizationResult: AuthorizationResult, context: Context): Drive {
        val accessToken = authorizationResult.accessToken
        val credentials = GoogleCredentials.create(AccessToken(accessToken!!, null))
        credentials.refreshIfExpired()
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }

    fun isGrantedAllScopes(authorizationResult: AuthorizationResult): Boolean {
        val grantedScopes = authorizationResult.grantedScopes.filter { grantedScope ->
            grantedScope.equals(DriveScopes.DRIVE_FILE) || grantedScope.equals(
                DriveScopes.DRIVE_APPDATA
            )
        }
        return grantedScopes.size == 2
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }

    fun checkForGooglePlayServices(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return status == ConnectionResult.SUCCESS
    }
}