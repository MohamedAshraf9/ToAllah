package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.google.android.exoplayer2.Timeline.Window
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.ActivityAdhenAlarmBinding

class AdhenAlarmActivity : AppCompatActivity() {
    lateinit var binding : ActivityAdhenAlarmBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdhenAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val mp = MediaPlayer.create(applicationContext, R.raw.azan1)
        mp.start()

        binding.stop.setOnClickListener {
            mp.stop()
        }
    }
}