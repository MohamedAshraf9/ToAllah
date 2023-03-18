package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import com.megahed.eqtarebmenalla.R

class Alert internal constructor(private val activity: Activity) {
    private lateinit var alertDialog : AlertDialog
    fun startLoadingAlert() {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        builder.setView(inflater.inflate(R.layout.adhen_alert, null))
        builder.setCancelable(true)
        alertDialog = builder.create()
        alertDialog.show()
    }

    fun dismissDialog() {
        alertDialog.cancel()

    }

    fun showDialog() {
        alertDialog.show()

    }
}