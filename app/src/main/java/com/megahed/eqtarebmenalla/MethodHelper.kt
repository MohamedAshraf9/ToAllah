package com.megahed.eqtarebmenalla

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

object MethodHelper {


    //const val currentTime = System.currentTimeMillis()

    fun shareApp(context: Context, textShare: String?, shareLink: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareLink)
        intent.putExtra(Intent.EXTRA_SUBJECT, textShare)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.shareApp)))
    }

    fun toastMessage(m: String?) {
        Toast.makeText(App.getInstance(), m, Toast.LENGTH_LONG).show()
    }
    fun isOnline(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }




}