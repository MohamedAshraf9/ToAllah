package com.megahed.eqtarebmenalla

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

object MethodHelper {


    //const val currentTime = System.currentTimeMillis()





    fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }




}