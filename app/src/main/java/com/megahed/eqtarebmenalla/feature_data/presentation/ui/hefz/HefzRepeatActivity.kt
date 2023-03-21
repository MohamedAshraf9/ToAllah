package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.ActivityHefzRepeatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyaViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HefzRepeatActivity : AppCompatActivity() , MenuProvider {

    private lateinit var binding: ActivityHefzRepeatBinding

    private var link:String?=null
    private var soraId:String?=null
    private var startAya:String?=null
    private var readerName:String?=null
    private var endAya:String?=null
    private var ayaRepeat:Int?=null
    private var allRepeat:Int?=null
    private lateinit var quranTextAdapter : AyaHefzAdapter

    lateinit var  player : ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHefzRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        link = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).link }
        soraId = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).soraId }
        startAya = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).startAya }
        endAya = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).endAya }
        ayaRepeat = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).ayaRepeat }
        allRepeat = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).allRepeat }
        readerName = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).readerName }


        player = ExoPlayer.Builder(this).build()







        val toolbar: Toolbar = binding.toolbar.toolbar
         setSupportActionBar(toolbar)
         supportActionBar?.setDisplayShowHomeEnabled(true)
         supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title=getString(R.string.listeningToSave)

        val verticalLayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= AyaHefzAdapter(this)

        binding.recyclerView.adapter = quranTextAdapter


        val ayaViewModel =
            ViewModelProvider(this).get(AyaViewModel::class.java)

        soraId?.let { it ->
            lifecycleScope.launchWhenStarted {



                ayaViewModel.getAyaOfSoraId(it.toInt()).collect { it1 ->
                    val ayaList = mutableListOf<Aya>()
                    for (i in startAya?.toInt()!! - 1 until endAya?.toInt()!!) {
                        ayaList.add(it1[i])
                    }
                    quranTextAdapter.setData(ayaList)


                }


            }
            for (k in 0 until allRepeat!! ) {
                for (i in startAya!!.toInt() .. endAya?.toInt()!!) {
                    for (j in 0 until ayaRepeat!!) {
                        val audioItem: MediaItem = MediaItem.fromUri(
                            Uri.parse(link!! + CommonUtils.convertSora(
                            soraId!!,
                            i.toString()
                        ) + ".mp3"
                        ))


                        player.addMediaItem(audioItem)
                        player.prepare()
                        player.play()

                    }
                }
            }
            

        }

        val menuHost: MenuHost = this

        menuHost.addMenuProvider(this, this, Lifecycle.State.RESUMED)

        binding.cancel.setOnClickListener {
            player.stop()
           finish()
        }
        




    }





    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return  when (menuItem.itemId) {
            android.R.id.home -> {
                player.stop()
                finish()
                true
            }
            else -> false
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        player.stop()
    }


}