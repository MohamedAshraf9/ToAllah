package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.Manifest
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzPagerAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.databinding.ActivityHefzRepeatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HefzRepeatActivity : AppCompatActivity(), MenuProvider, Player.Listener {

    private lateinit var binding: ActivityHefzRepeatBinding

    private var link: String? = null
    private var soraId: String? = null
    private var startAya: String? = null
    private var endAya: String? = null
    private var readerName: String? = null
    private var ayaRepeat: Int? = null
    private var allRepeat: Int? = null
    private var skipToNext = false

    private lateinit var ayaHefzPagerAdapter: AyaHefzPagerAdapter
    private var ayaList = mutableListOf<Aya>()

    private var isMemorizationStopped = false
    private var isMemorizationPaused = false
    private var currentVerseIndex = 0
    private var currentRepeatIndex = 0

    private lateinit var sharedPreferences: SharedPreferences
    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var dataSourceFactory: DefaultDataSource.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHefzRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("playback_prefs", MODE_PRIVATE)
        extractIntentExtras()
        setupToolbar()
        setupViewPager()
        initializePlayer()

        val ayaViewModel = ViewModelProvider(this)[AyaViewModel::class.java]
        observeConnection(ayaViewModel)

        val menuHost: MenuHost = this
        menuHost.addMenuProvider(this, this, Lifecycle.State.RESUMED)

        setupClickListeners()
        updatePauseResumeButtonState()
    }

    private fun initializePlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        dataSourceFactory = DefaultDataSource.Factory(this)
        exoPlayer?.addListener(this)
    }

    override fun onBackPressed() {
        stopMemorization()
        super.onBackPressed()
    }

    override fun onDestroy() {
        stopMemorization()
        exoPlayer?.release()
        exoPlayer = null
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
        toolbar.title = getString(R.string.listening_to_save)
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
                    startMemorizationProcess()
                }
            }
        }
    }

    private fun startMemorizationProcess() {
        CoroutineScope(Dispatchers.Main).launch {
            currentVerseIndex = 0
            for (k in 0 until allRepeat!!) {
                if (isMemorizationStopped) break
                for (i in 0 until ayaList.size) {
                    if (isMemorizationStopped) break
                    currentVerseIndex = i
                    val aya = ayaList[i]
                    binding.viewPager.setCurrentItem(i, true)
                    delay(300)
                    for (j in 0 until ayaRepeat!!) {
                        if (isMemorizationStopped) break
                        currentRepeatIndex = j
                        while (isMemorizationPaused && !isMemorizationStopped) {
                            delay(100)
                        }
                        if (isMemorizationStopped) break
                        playAya(aya)
                        awaitAyaCompletion()
                        if (skipToNext) {
                            skipToNext = false
                            break
                        }
                        if (j < ayaRepeat!! - 1) delay(500)
                    }
                }
                if (!isMemorizationStopped) {
                    sharedPreferences.edit { putInt("all_repeat_counter", k + 1) }
                    delay(1000)
                }
            }
            if (!isMemorizationStopped) {
                showMessage(this@HefzRepeatActivity, getString(R.string.memorization_completed))
                updateUIForCompletion()
            }
        }
    }

    private suspend fun awaitAyaCompletion() {
        exoPlayer?.let { player ->
            var waitCount = 0
            val maxWaitCount = 500

            while (player.playbackState != Player.STATE_READY &&
                player.playbackState != Player.STATE_BUFFERING &&
                waitCount < maxWaitCount &&
                !isMemorizationStopped &&
                !skipToNext) {
                delay(100)
                waitCount++
            }

            if (skipToNext) {
                player.stop()
                return
            }

            waitCount = 0
            while (player.playbackState != Player.STATE_ENDED &&
                waitCount < maxWaitCount &&
                !isMemorizationStopped &&
                !skipToNext) {
                while (isMemorizationPaused && !isMemorizationStopped && !skipToNext) {
                    delay(100)
                }
                if (isMemorizationStopped || skipToNext) break
                delay(100)
                waitCount++
            }

            if (skipToNext || waitCount >= maxWaitCount) player.stop()
        }
    }

    private fun updateUIForCompletion() {
        runOnUiThread {
            binding.stopMemorization.isEnabled = false
            binding.skipForwardButton.isEnabled = false
            binding.cancel.isEnabled = true
            binding.pauseResumeButton.text = getString(R.string.repeat)
            binding.pauseResumeButton.icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_repeat, theme)
            binding.pauseResumeButton.isEnabled = true
            binding.pauseResumeButton.setOnClickListener { repeatMemorization() }
        }
    }

    private fun repeatMemorization() {
        isMemorizationStopped = false
        isMemorizationPaused = false
        skipToNext = false

        binding.stopMemorization.isEnabled = true
        binding.skipForwardButton.isEnabled = true
        binding.cancel.isEnabled = true

        binding.pauseResumeButton.text = getString(R.string.pause)
        binding.pauseResumeButton.icon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, theme)
        binding.pauseResumeButton.setOnClickListener { togglePauseResume() }
        exoPlayer?.stop()
        binding.viewPager.setCurrentItem(0, true)
        startMemorizationProcess()
    }

    private fun playAya(aya: Aya) {
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItem = MediaItem.fromUri(Uri.parse(aya.url))
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                player.playWhenReady = true
            }
        }
    }

    private fun togglePauseResume() {
        if (isMemorizationPaused) {
            isMemorizationPaused = false
            exoPlayer?.playWhenReady = true
            if (binding.viewPager.currentItem != currentVerseIndex) {
                binding.viewPager.setCurrentItem(currentVerseIndex, true)
            }
        } else {
            isMemorizationPaused = true
            exoPlayer?.playWhenReady = false
        }
        updatePauseResumeButtonState()
    }

    private fun updatePauseResumeButtonState() {
        if (isMemorizationPaused) {
            binding.pauseResumeButton.text = getString(R.string.resume)
            binding.pauseResumeButton.icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_play, theme)
        } else {
            binding.pauseResumeButton.text = getString(R.string.pause)
            binding.pauseResumeButton.icon =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, theme)
        }
    }

    private fun skipToNextVerse() {
        skipToNext = true
        exoPlayer?.stop()
    }

    private fun stopMemorization() {
        isMemorizationStopped = true
        isMemorizationPaused = false
        exoPlayer?.stop()

        binding.stopMemorization.apply {
            isEnabled = false
            text = getString(R.string.stopped)
        }
        binding.pauseResumeButton.apply {
            isEnabled = false
            text = getString(R.string.stopped)
            icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_stop, theme)
        }
        binding.skipForwardButton.isEnabled = false
    }

    private fun updatePositionIndicator(position: Int) {
        val currentAya = position + 1
        val totalAyas = ayaList.size
        binding.positionIndicator.text = getString(R.string.position_format, currentAya, totalAyas)
    }

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
