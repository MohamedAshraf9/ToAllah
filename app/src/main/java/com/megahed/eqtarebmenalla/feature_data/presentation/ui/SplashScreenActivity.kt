package com.megahed.eqtarebmenalla.feature_data.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.megahed.eqtarebmenalla.MainActivity
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.ThemeHelper

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private val TIMER: Short = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        ThemeHelper.applyTheme(settings)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, TIMER.toLong())
    }
}