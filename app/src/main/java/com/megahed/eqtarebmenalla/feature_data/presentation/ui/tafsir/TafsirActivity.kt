package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tafsir

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Observer
import com.megahed.eqtarebmenalla.databinding.ActivityTafsirBinding
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.TafsirVM

class TafsirActivity : AppCompatActivity() {

    lateinit var binding : ActivityTafsirBinding
    lateinit var tafsirVM: TafsirVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTafsirBinding.inflate(layoutInflater)
        setContentView(binding.root)


        tafsirVM = TafsirVM()

        var suraId : String = intent.getStringExtra("sura").toString()
        var ayaId : String = intent.getStringExtra("eyaId").toString()
        var aya : String = intent.getStringExtra("aya").toString()

        tafsirVM.getTafsir(suraId, ayaId).observe(this, Observer {
            binding.tafsir.text = it.text
            binding.aya.text = aya
        })






    }
}