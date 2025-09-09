package com.megahed.eqtarebmenalla.offline

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

object OfflineUtils {

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatFileSize(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    fun getAvailableStorage(context: Context): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    fun getTotalDownloadSize(context: Context, readerId: String): Long {
        val folder = File(context.filesDir, "offline_audio")
        if (!folder.exists()) return 0L

        return folder.walkTopDown()
            .filter { it.isFile && it.name.contains(readerId) }
            .map { it.length() }
            .sum()
    }
}

