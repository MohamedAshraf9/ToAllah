package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.databinding.ActivityHefzRepeatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


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

    private val hefzRepeatViewModel: HefzRepeatViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHefzRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

        link = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).link }
        soraId = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).soraId }
        startAya = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).startAya }
        endAya = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).endAya }
        ayaRepeat = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).ayaRepeat }
        allRepeat = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).allRepeat }
        readerName = intent.extras?.let { HefzRepeatActivityArgs.fromBundle(it).readerName }



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
            hefzRepeatViewModel.isConnected.observe(this@HefzRepeatActivity) { eventResource ->
                eventResource?.peekContent()?.let { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            if (resource.data == true) {

                                val ayaList = mutableListOf<Aya>()

                                lifecycleScope.launchWhenStarted {
                                    ayaViewModel.getAyaOfSoraId(it.toInt()).collect { it1 ->
                                        for (i in startAya?.toInt()!! - 1 until endAya?.toInt()!!) {
                                            it1[i].url = link!! + CommonUtils.convertSora(
                                                soraId!!,
                                                (i + 1).toString()
                                            ) + ".mp3"
                                            ayaList.add(it1[i])
                                        }
                                        quranTextAdapter.setData(ayaList)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            launchNotificationPermission()
                                        }
                                        FirebaseMusicSource._ayasLiveData.value = ayaList


                                        CoroutineScope(Dispatchers.Main).launch {
                                            for (k in 0 until allRepeat!!) {
                                                for (i in startAya!!.toInt()..endAya?.toInt()!!) {
                                                    //for (j in 0 until ayaRepeat!!) {
                                                        val aya = ayaList[i - startAya!!.toInt()] // Adjust Aya index for startAya
                                                        hefzRepeatViewModel.playOrToggleAya(aya)
                                                }
                                                sharedPreferences.edit().putInt("all_repeat_counter", k + 1).apply()
                                                awaitAyaCompletion()
                                            }
                                        }
                                        }
                                    }


                                }
                            }
                        is Resource.Error -> {
                            Log.e("PlaybackDebug", "Connection error: ${resource.message}")
                        }
                        is Resource.Loading -> {
                            Log.d("PlaybackDebug", "Connecting...")
                        }
                    }
                }
            }
        }

        val menuHost: MenuHost = this

        menuHost.addMenuProvider(this, this, Lifecycle.State.RESUMED)

        binding.cancel.setOnClickListener {
           finish()
        }





    }

    private suspend fun awaitAyaCompletion() {
        var hasStartedPlaying = false

        while (true) {
            val playbackState = hefzRepeatViewModel.playbackState.value?.state

            // Wait for playback to start
            if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                hasStartedPlaying = true
            }

            // Once playback has started, wait for it to stop or pause
            if (hasStartedPlaying &&
                (playbackState == PlaybackStateCompat.STATE_STOPPED ||
                        playbackState == PlaybackStateCompat.STATE_PAUSED ||
                        playbackState == PlaybackStateCompat.STATE_NONE)) {
                break
            }

            delay(100) // Polling interval for state change
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun launchNotificationPermission(){
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            )
        } else {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        if (!MethodHelper.hasPermissions(this, perms)){
            requestPermissionLauncher.launch(perms)
        }

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        // }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all {
            it.value
        }
        if (!granted) {
            // PERMISSION GRANTED
            showMessage(this,getString(R.string.need_permissions))
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return  when (menuItem.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> false
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }


}