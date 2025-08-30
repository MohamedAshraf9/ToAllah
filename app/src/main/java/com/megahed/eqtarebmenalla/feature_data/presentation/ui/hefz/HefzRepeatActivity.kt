package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.Manifest
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
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzPagerAdapter
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HefzRepeatActivity : AppCompatActivity(), MenuProvider {

    private lateinit var binding: ActivityHefzRepeatBinding

    private var link: String? = null
    private var soraId: String? = null
    private var startAya: String? = null
    private var endAya: String? = null
    private var readerName: String? = null
    private var ayaRepeat: Int? = null
    private var allRepeat: Int? = null

    private lateinit var ayaHefzPagerAdapter: AyaHefzPagerAdapter
    private var ayaList = mutableListOf<Aya>()

    private var isMemorizationStopped = false
    private var isMemorizationPaused = false

    private val hefzRepeatViewModel: HefzRepeatViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHefzRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("playback_prefs", MODE_PRIVATE)
        extractIntentExtras()
        setupToolbar()
        setupViewPager()

        val ayaViewModel = ViewModelProvider(this)[AyaViewModel::class.java]
        observeConnection(ayaViewModel)

        val menuHost: MenuHost = this
        menuHost.addMenuProvider(this, this, Lifecycle.State.RESUMED)

        setupClickListeners()
        updatePauseResumeButtonState()
    }

    override fun onBackPressed() {
        stopMemorization()
        super.onBackPressed()
    }

    override fun onDestroy() {
        stopMemorization()
        super.onDestroy()
    }

    private fun extractIntentExtras() {
        intent.extras?.let {
            val args = HefzRepeatActivityArgs.fromBundle(it)
            link = args.link
            soraId = args.soraId
            startAya = args.startAya
            endAya = args.endAya
            ayaRepeat = args.ayaRepeat
            allRepeat = args.allRepeat
            readerName = args.readerName
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = getString(R.string.listeningToSave)
    }

    private fun setupViewPager() {
        ayaHefzPagerAdapter = AyaHefzPagerAdapter(this)
        binding.viewPager.adapter = ayaHefzPagerAdapter
        binding.viewPager.isUserInputEnabled = true

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePositionIndicator(position)
            }
        })
    }

    private fun setupClickListeners() {
        binding.cancel.setOnClickListener {
            stopMemorization()
            finish()
        }
        binding.stopMemorization.setOnClickListener { stopMemorization() }
        binding.pauseResumeButton.setOnClickListener { togglePauseResume() }
        binding.skipForwardButton.setOnClickListener { skipToNextVerse() }
    }

    private fun observeConnection(ayaViewModel: AyaViewModel) {
        soraId?.let { id ->
            hefzRepeatViewModel.isConnected.observe(this) { eventResource ->
                eventResource?.peekContent()?.let { resource ->
                    when (resource) {
                        is Resource.Success -> if (resource.data == true) {
                            lifecycleScope.launchWhenStarted {
                                ayaViewModel.getAyaOfSoraId(id.toInt()).collect { ayaResult ->
                                    ayaList.clear()
                                    for (i in startAya!!.toInt() - 1 until endAya!!.toInt()) {
                                        ayaResult[i].url = link!! + CommonUtils.convertSora(
                                            soraId!!, (i + 1).toString()
                                        ) + ".mp3"
                                        ayaList.add(ayaResult[i])
                                    }
                                    ayaHefzPagerAdapter.setData(ayaList)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        launchNotificationPermission()
                                    }
                                    FirebaseMusicSource._ayasLiveData.value = ayaList
                                    startMemorizationProcess()
                                }
                            }
                        }
                        is Resource.Error -> Log.e("PlaybackDebug", "Connection error: ${resource.message}")
                        is Resource.Loading -> Log.d("PlaybackDebug", "Connecting...")
                    }
                }
            }
        }
    }

    private fun startMemorizationProcess() {
        CoroutineScope(Dispatchers.Main).launch {
            for (k in 0 until allRepeat!!) {
                if (isMemorizationStopped) break

                for (i in 0 until ayaList.size) {
                    if (isMemorizationStopped) break
                    val aya = ayaList[i]
                    binding.viewPager.setCurrentItem(i, true)

                    for (j in 0 until ayaRepeat!!) {
                        if (isMemorizationStopped) break
                        while (isMemorizationPaused && !isMemorizationStopped) delay(100)
                        if (isMemorizationStopped) break

                        hefzRepeatViewModel.playOrToggleAya(aya)
                        awaitAyaCompletion()
                        if (j < ayaRepeat!! - 1) delay(500)
                    }
                }
                if (!isMemorizationStopped) {
                    sharedPreferences.edit { putInt("all_repeat_counter", k + 1) }
                    delay(1000)
                }
            }
            if (!isMemorizationStopped) {
                showMessage(this@HefzRepeatActivity, "تم إكمال الحفظ بنجاح")
            }
        }
    }

    private suspend fun awaitAyaCompletion() {
        var hasStartedPlaying = false
        while (true) {
            if (isMemorizationStopped) break
            while (isMemorizationPaused && !isMemorizationStopped) delay(100)
            if (isMemorizationStopped) break

            val playbackState = hefzRepeatViewModel.playbackState.value?.state
            if (playbackState == PlaybackStateCompat.STATE_PLAYING) hasStartedPlaying = true
            if (hasStartedPlaying &&
                (playbackState == PlaybackStateCompat.STATE_STOPPED ||
                        playbackState == PlaybackStateCompat.STATE_PAUSED ||
                        playbackState == PlaybackStateCompat.STATE_NONE)
            ) break

            delay(100)
        }
    }

    private fun togglePauseResume() {
        if (isMemorizationPaused) {
            isMemorizationPaused = false
            hefzRepeatViewModel.resumePlayback()
        } else {
            isMemorizationPaused = true
            hefzRepeatViewModel.pausePlayback()
        }
        updatePauseResumeButtonState()
    }

    private fun updatePauseResumeButtonState() {
        if (isMemorizationPaused) {
            binding.pauseResumeButton.text = "استكمال"
            binding.pauseResumeButton.icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play, theme)
        } else {
            binding.pauseResumeButton.text = "إيقاف مؤقت"
            binding.pauseResumeButton.icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, theme)
        }
    }

    private fun skipToNextVerse() {
        val repeats = ayaRepeat ?: 1
        val current = binding.viewPager.currentItem
        if (current < ayaList.size - 1) {
            binding.viewPager.setCurrentItem(current + 1, true)
            repeat(repeats) { hefzRepeatViewModel.skipToNextAya() }
        }
    }

    private fun stopMemorization() {
        isMemorizationStopped = true
        isMemorizationPaused = false
        hefzRepeatViewModel.stopPlayback()

        binding.stopMemorization.apply {
            isEnabled = false
            text = "تم الإيقاف"
        }
        binding.pauseResumeButton.apply {
            isEnabled = false
            text = "تم الإيقاف"
            icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_stop, theme)
        }
        binding.skipForwardButton.isEnabled = false
    }

    private fun updatePositionIndicator(position: Int) {
        val currentAya = position + 1
        val totalAyas = ayaList.size
        binding.positionIndicator.text = "$currentAya / $totalAyas"
    }

    // === Permissions ===
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun launchNotificationPermission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            )
        } else {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!MethodHelper.hasPermissions(this, perms)) {
            requestPermissionLauncher.launch(perms)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (!granted) showMessage(this, getString(R.string.need_permissions))
        }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                stopMemorization()
                finish()
                true
            }
            else -> false
        }
    }
}
