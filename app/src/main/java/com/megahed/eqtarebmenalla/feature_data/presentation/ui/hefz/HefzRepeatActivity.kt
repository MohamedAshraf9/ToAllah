package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.Manifest
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzPagerAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.ActivityHefzRepeatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.SessionType
import com.megahed.eqtarebmenalla.db.model.getTotalVerses
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyaViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import com.megahed.eqtarebmenalla.offline.SmartAudioUrlHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HefzRepeatActivity : AppCompatActivity(), MenuProvider, Player.Listener {

    private lateinit var binding: ActivityHefzRepeatBinding
    private lateinit var memorizationViewModel: MemorizationViewModel

    @Inject
    lateinit var offlineAudioManager: OfflineAudioManager
    private var accumulatedVersesCompleted = 0
    private var readerId: String? = null
    private var baseUrl: String? = null
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
    private var exoPlayer: ExoPlayer? = null
    private lateinit var dataSourceFactory: DefaultDataSource.Factory

    private var isOfflineMode = false
    private var lastFailedVerseIndex = -1
    private var hasShownOfflineWarning = false
    private var waitingForUserDecision = false
    private var userRequestedRetry = false

    private var sessionId: Long? = null
    private var sessionStartTime: Date? = null
    private var versesCompleted = 0
    private var isProgressTrackingEnabled = false
    private var totalVersesInTarget = 0
    private var initialCompletedVerses = 0
    private var targetStartVerse = 0
    private var targetEndVerse = 0
    private var sessionStartVerse = 0
    private var sessionEndVerse = 0
    private var isDialogShowing = false
    private var hasShownCompletionDialog = false
    private var userChoiceMade = false
    private var userWantsAutoComplete = false
    private var completionDialogShown = false

    private var shouldSaveProgress = true
    private var sessionStartProgress = 0
    private var pendingProgressUpdates = mutableListOf<Int>()
    private val progressUpdateMutex = Mutex()
    private var isExitingWithoutSave = false
    private var lastSavedProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHefzRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memorizationViewModel = ViewModelProvider(this)[MemorizationViewModel::class.java]

        sharedPreferences = getSharedPreferences("playback_prefs", MODE_PRIVATE)
        extractIntentExtras()
        checkOfflineMode()
        setupToolbar()
        setupViewPager()
        initializePlayer()

        val ayaViewModel = ViewModelProvider(this)[AyaViewModel::class.java]
        observeConnection(ayaViewModel)

        val menuHost: MenuHost = this
        menuHost.addMenuProvider(this, this, Lifecycle.State.RESUMED)

        setupClickListeners()
        updatePauseResumeButtonState()

        setupProgressObservers()
        checkProgressTrackingEligibility()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isProgressTrackingEnabled && sessionId != null) {
                    showExitConfirmationDialog()
                } else {
                    stopMemorization()
                    finish()
                }
            }
        })
    }

    private fun extractIntentExtras() {
        intent.extras?.let {
            val args = HefzRepeatActivityArgs.fromBundle(it)
            baseUrl = args.link
            soraId = args.soraId
            startAya = args.startAya
            endAya = args.endAya
            ayaRepeat = args.ayaRepeat
            allRepeat = args.allRepeat
            readerName = args.readerName
            readerId = args.readerId
        }
    }

    private fun observeConnection(ayaViewModel: AyaViewModel) {
        soraId?.let { id ->
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    ayaViewModel.getAyaOfSoraId(id.toInt()).collect { ayaResult ->
                        ayaList.clear()
                        for (i in startAya!!.toInt() - 1 until endAya!!.toInt()) {
                            val aya = ayaResult[i]
                            val verseNumber = i + 1
                            lifecycleScope.launch {
                                aya.url = SmartAudioUrlHelper.getAudioUrl(
                                    offlineAudioManager = offlineAudioManager,
                                    readerId = normalizeToAsciiDigits(readerId ?: ""),
                                    surahId = id.toInt(),
                                    verseId = verseNumber,
                                    onlineBaseUrl = normalizeUrl(baseUrl ?: "")
                                )
                            }
                            ayaList.add(aya)
                        }
                        ayaHefzPagerAdapter.setData(ayaList)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launchNotificationPermission()
                        }
                        lifecycleScope.launch {
                            showPreSessionMissingVersesDialog()
                        }
                    }
                }
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }

    private fun switchToOnlineMode() {
        if (!offlineAudioManager.isNetworkAvailable()) {
            Snackbar.make(binding.root, "لا يوجد اتصال بالإنترنت", Snackbar.LENGTH_LONG).show()
            return
        }
        isOfflineMode = false
        sharedPreferences.edit { putBoolean("is_offline_mode", false) }
        binding.toolbar.toolbar.subtitle = "الوضع المتصل نشط"
        lifecycleScope.launch {
            try {
                val readerId = normalizeToAsciiDigits(extractReaderIdFromIntent() ?: "")
                val baseUrl = getOnlineBaseUrl(readerId)
                ayaList.forEachIndexed { index, aya ->
                    val verseNumber = startAya!!.toInt() + index
                    val surahFormatted = String.format(Locale.US, "%03d", soraId!!.toInt())
                    val verseFormatted = String.format(Locale.US, "%03d", verseNumber)
                    aya.url = "$baseUrl/${surahFormatted}${verseFormatted}.mp3"
                }
                ayaHefzPagerAdapter.notifyDataSetChanged()
                Snackbar.make(binding.root, "تم التبديل للوضع المتصل", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "فشل في التبديل للوضع المتصل", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun downloadMissingVerses(readerId: String, surahId: Int, missingVerses: List<Int>) {
        if (!offlineAudioManager.isNetworkAvailable()) {
            Snackbar.make(binding.root, "لا يوجد اتصال بالإنترنت", Snackbar.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            try {
                var downloadedCount = 0
                val totalMissing = missingVerses.size
                binding.toolbar.toolbar.subtitle = "جاري تحميل الآيات المفقودة..."
                for (verseId in missingVerses) {
                    val normalizedReaderId = normalizeToAsciiDigits(readerId)
                    val baseUrl = getOnlineBaseUrl(normalizedReaderId)
                    val surahFormatted = String.format(Locale.US, "%03d", surahId)
                    val verseFormatted = String.format(Locale.US, "%03d", verseId)
                    val verseUrl = "$baseUrl/${surahFormatted}${verseFormatted}.mp3"
                    val verseName = "${getSurahName(surahId)}_آية_${verseId}"
                    val success = offlineAudioManager.downloadVerseAudio(
                        readerId = normalizedReaderId,
                        surahId = surahId,
                        verseId = verseId,
                        verseName = verseName,
                        readerName = readerName ?: "Unknown",
                        audioUrl = verseUrl
                    )
                    if (success) {
                        downloadedCount++
                        binding.toolbar.toolbar.subtitle = "تحميل: $downloadedCount/$totalMissing"
                    }
                }
                if (downloadedCount == totalMissing) {
                    binding.toolbar.toolbar.subtitle = "تم التحميل - وضع عدم الاتصال نشط"
                    Snackbar.make(
                        binding.root,
                        "تم تحميل جميع الآيات المفقودة",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    binding.toolbar.toolbar.subtitle = "تحميل جزئي - $downloadedCount/$totalMissing"
                    Snackbar.make(
                        binding.root,
                        "تم تحميل $downloadedCount من $totalMissing آية",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "فشل في تحميل الآيات المفقودة", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        dataSourceFactory = DefaultDataSource.Factory(this)
        exoPlayer?.addListener(this)
    }

    @OptIn(UnstableApi::class)
    private suspend fun playAya(aya: Aya): Boolean {
        return try {
            val player = exoPlayer ?: return false
            player.stop()
            player.clearMediaItems()
            if (aya.url.isNullOrEmpty()) {
                handleMissingAudio(currentVerseIndex + 1)
                return false
            }
            if (isOfflineMode) {
                val file = File(aya.url!!)
                if (!file.exists() || file.length() == 0L) {
                    handleMissingAudio(currentVerseIndex + 1)
                    return false
                }
            }
            val mediaItem = if (aya.url?.startsWith("http") == true) {
                if (!offlineAudioManager.isNetworkAvailable()) {
                    handleNetworkError(currentVerseIndex + 1)
                    return false
                }
                MediaItem.fromUri(Uri.parse(aya.url))
            } else {
                MediaItem.fromUri(Uri.fromFile(File(aya.url)))
            }
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            var retryCount = 0
            while (player.playbackState == Player.STATE_IDLE && retryCount < 50) {
                delay(100)
                retryCount++
            }
            if (player.playbackState == Player.STATE_IDLE) {
                handlePlaybackError(currentVerseIndex + 1)
                return false
            }
            player.playWhenReady = true
            true
        } catch (e: Exception) {
            handlePlaybackError(currentVerseIndex + 1, e.message)
            false
        }
    }

    private fun handleMissingAudio(verseNumber: Int) {
        lastFailedVerseIndex = currentVerseIndex
        val surahName = Constants.SORA_OF_QURAN[soraId?.toInt() ?: 0]
        runOnUiThread {
            if (!hasShownOfflineWarning) {
                hasShownOfflineWarning = true
                waitingForUserDecision = true
                userRequestedRetry = false
                MaterialAlertDialogBuilder(this)
                    .setTitle("آية غير متوفرة")
                    .setMessage("الآية رقم $verseNumber من $surahName غير متوفرة للتشغيل في وضع عدم الاتصال.\n\nهل تريد:")
                    .setPositiveButton("تخطي الآيات المفقودة") { _, _ ->
                        hasShownOfflineWarning = false
                        waitingForUserDecision = false
                        userRequestedRetry = false
                        lastFailedVerseIndex = -1
                    }
                    .setNegativeButton("إيقاف الحفظ") { _, _ ->
                        waitingForUserDecision = false
                        userRequestedRetry = false
                        stopMemorization()
                    }
                    .setNeutralButton("المحاولة مرة أخرى") { _, _ ->
                        hasShownOfflineWarning = false
                        if (offlineAudioManager.isNetworkAvailable()) {
                            userRequestedRetry = true
                            waitingForUserDecision = false
                            attemptToDownloadMissingVerse(verseNumber)
                        } else {
                            Snackbar.make(
                                binding.root,
                                "لا يوجد اتصال بالإنترنت",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun handleNetworkError(verseNumber: Int) {
        lastFailedVerseIndex = currentVerseIndex
        waitingForUserDecision = true
        userRequestedRetry = false
        runOnUiThread {
            MaterialAlertDialogBuilder(this)
                .setTitle("لا يوجد اتصال بالإنترنت")
                .setMessage("فشل في تشغيل الآية رقم $verseNumber بسبب عدم وجود اتصال بالإنترنت.")
                .setPositiveButton("إعادة المحاولة") { _, _ ->
                    if (offlineAudioManager.isNetworkAvailable()) {
                        userRequestedRetry = true
                        waitingForUserDecision = false
                    } else {
                        handleNetworkError(verseNumber)
                    }
                }
                .setNegativeButton("إيقاف") { _, _ ->
                    waitingForUserDecision = false
                    userRequestedRetry = false
                    stopMemorization()
                }
                .show()
        }
    }

    private fun handlePlaybackError(verseNumber: Int, errorMessage: String? = null) {
        lastFailedVerseIndex = currentVerseIndex
        runOnUiThread {
            val message = if (errorMessage != null) {
                "خطأ في تشغيل الآية رقم $verseNumber:\n$errorMessage"
            } else {
                "فشل في تشغيل الآية رقم $verseNumber"
            }
            MaterialAlertDialogBuilder(this)
                .setTitle("خطأ في التشغيل")
                .setMessage(message)
                .setPositiveButton("تخطي") { _, _ ->
                    resumeFromCurrentPosition()
                }
                .setNegativeButton("إيقاف") { _, _ ->
                    stopMemorization()
                }
                .show()
        }
    }

    private fun attemptToDownloadMissingVerse(verseNumber: Int) {
        lifecycleScope.launch {
            try {
                val readerId = normalizeToAsciiDigits(extractReaderIdFromIntent() ?: "")
                val surahId = soraId!!.toInt()
                val surahName = Constants.SORA_OF_QURAN[surahId]
                val baseUrl = getOnlineBaseUrl(readerId)
                val surahFormatted = String.format(Locale.US, "%03d", surahId)
                val verseFormatted = String.format(Locale.US, "%03d", verseNumber)
                val verseUrl = "$baseUrl/${surahFormatted}${verseFormatted}.mp3"
                val verseName = "${surahName}_آية_${verseNumber}"
                binding.toolbar.toolbar.subtitle = "جاري تحميل الآية $verseNumber..."
                val success = offlineAudioManager.downloadVerseAudio(
                    readerId = readerId,
                    surahId = surahId,
                    verseId = verseNumber,
                    verseName = verseName,
                    readerName = readerName ?: "Unknown",
                    audioUrl = verseUrl
                )
                if (success) {
                    val localPath = offlineAudioManager.getVerseAudioPath(
                        readerId,
                        soraId?.toInt() ?: return@launch,
                        verseNumber
                    )
                    if (localPath != null) {
                        ayaList.find { it.ayaId == verseNumber }?.url = localPath
                    }
                    binding.toolbar.toolbar.subtitle = "تم تحميل الآية - وضع عدم الاتصال نشط"
                    Snackbar.make(
                        binding.root,
                        "تم تحميل الآية $verseNumber بنجاح",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    userRequestedRetry = true
                    waitingForUserDecision = false
                } else {
                    binding.toolbar.toolbar.subtitle = "فشل التحميل - وضع عدم الاتصال نشط"
                    handleMissingAudio(verseNumber)
                }
            } catch (e: Exception) {
                binding.toolbar.toolbar.subtitle = "فشل التحميل - وضع عدم الاتصال نشط"
                Snackbar.make(
                    binding.root,
                    "خطأ في تحميل الآية: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                resumeFromCurrentPosition()
            }
        }
    }

    private fun resumeFromCurrentPosition() {
        userRequestedRetry = true
        waitingForUserDecision = false
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
                    if (isProgressTrackingEnabled) {
                        versesCompleted = i + 1
                        updateProgressDisplay()
                    }
                    for (j in 0 until ayaRepeat!!) {
                        if (isMemorizationStopped) break
                        currentRepeatIndex = j
                        while (isMemorizationPaused && !isMemorizationStopped) {
                            delay(100)
                        }
                        if (isMemorizationStopped) break
                        var playbackSuccess = playAya(aya)
                        if (!playbackSuccess) {
                            while (waitingForUserDecision && !isMemorizationStopped) {
                                delay(100)
                            }
                            if (isMemorizationStopped) break
                            var retryAttempts = 0
                            val maxRetries = 3
                            while (!playbackSuccess && retryAttempts < maxRetries && !isMemorizationStopped) {
                                playbackSuccess = playAya(aya)
                                if (!playbackSuccess) {
                                    while (waitingForUserDecision && !isMemorizationStopped) {
                                        delay(100)
                                    }
                                    if (isMemorizationStopped) break
                                    if (!userRequestedRetry) {
                                        break
                                    } else {
                                        userRequestedRetry = false
                                        retryAttempts++
                                    }
                                }
                            }
                            if (!playbackSuccess && !isMemorizationStopped) {
                                continue
                            }
                            if (isMemorizationStopped) break
                        }
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
                if (isProgressTrackingEnabled && sessionId != null) {
                    showAutoCompletionDialog()
                }
                updateUIForCompletion()
            }
        }
    }

    private suspend fun checkVerseAvailability(): List<Int> {
        val missingVerses = mutableListOf<Int>()
        val readerId = normalizeToAsciiDigits(extractReaderIdFromIntent() ?: "")
        val surahId = soraId!!.toInt()
        val startVerse = startAya!!.toInt()
        val endVerse = endAya!!.toInt()
        if (isOfflineMode) {
            for (verseNumber in startVerse..endVerse) {
                val isAvailable =
                    offlineAudioManager.isVerseAudioDownloaded(readerId, surahId, verseNumber)
                if (!isAvailable) {
                    missingVerses.add(verseNumber)
                }
            }
        }
        return missingVerses
    }

    private suspend fun showPreSessionMissingVersesDialog() {
        if (isOfflineMode) {
            val missingVerses = checkVerseAvailability()
            if (missingVerses.isNotEmpty()) {
                val totalVerses = endAya!!.toInt() - startAya!!.toInt() + 1
                val availableVerses = totalVerses - missingVerses.size
                runOnUiThread {
                    MaterialAlertDialogBuilder(this@HefzRepeatActivity)
                        .setTitle("تحذير: آيات مفقودة")
                        .setMessage(
                            "في وضع عدم الاتصال:\n• متوفر: $availableVerses من $totalVerses آية\n• مفقود: ${
                                missingVerses.joinToString(
                                    ", "
                                )
                            }\n\nستتوقف الجلسة عند الآيات المفقودة. هل تريد المتابعة؟"
                        )
                        .setPositiveButton("متابعة") { _, _ ->
                            startMemorizationProcess()
                        }
                        .setNegativeButton("إلغاء") { _, _ ->
                            finish()
                        }
                        .setNeutralButton("تحميل المفقود") { _, _ ->
                            if (offlineAudioManager.isNetworkAvailable()) {
                                downloadMissingVerses(
                                    extractReaderIdFromIntent() ?: "",
                                    soraId!!.toInt(),
                                    missingVerses
                                )
                            } else {
                                Snackbar.make(
                                    binding.root,
                                    "لا يوجد اتصال بالإنترنت",
                                    Snackbar.LENGTH_LONG
                                ).show()
                                startMemorizationProcess()
                            }
                        }
                        .setCancelable(false)
                        .show()
                }
                return
            }
        }
        startMemorizationProcess()
    }

    private fun checkOfflineMode() {
        isOfflineMode = sharedPreferences.getBoolean("is_offline_mode", false)
        if (isOfflineMode) {
            setupOfflineAudio()
        }
    }

    private fun setupOfflineAudio() {
        lifecycleScope.launch {
            try {
                val readerId = extractReaderIdFromIntent()
                if (readerId != null && soraId != null) {
                    val surahId = soraId!!.toInt()
                    val startVerse = startAya!!.toInt()
                    val endVerse = endAya!!.toInt()
                    val allDownloaded = offlineAudioManager.areVersesDownloaded(
                        readerId,
                        surahId,
                        startVerse,
                        endVerse
                    )
                    if (allDownloaded) {
                        binding.toolbar.toolbar.subtitle = "وضع عدم الاتصال نشط"
                    } else {
                        if (offlineAudioManager.isNetworkAvailable()) {
                            showOfflineUnavailableDialog(readerId, surahId, startVerse, endVerse)
                        } else {
                            showNoNetworkDialog()
                        }
                    }
                }
            } catch (e: Exception) {
                showOfflineUnavailableDialog("", 0, 0, 0)
            }
        }
    }

    private fun showOfflineUnavailableDialog(
        readerId: String,
        surahId: Int,
        startVerse: Int,
        endVerse: Int,
    ) {
        lifecycleScope.launch {
            val missingVerses =
                offlineAudioManager.getMissingVerses(readerId, surahId, startVerse, endVerse)
            val totalVerses = endVerse - startVerse + 1
            val availableVerses = totalVerses - missingVerses.size
            MaterialAlertDialogBuilder(this@HefzRepeatActivity)
                .setTitle("المحتوى غير متوفر بالكامل")
                .setMessage(
                    "متوفر $availableVerses من $totalVerses آية للوضع غير المتصل.\nالآيات المفقودة: ${
                        missingVerses.joinToString(
                            ", "
                        )
                    }\n\nهل تريد تحميل الآيات المفقودة أم التبديل للوضع المتصل؟"
                )
                .setPositiveButton("تحميل المفقود") { _, _ ->
                    downloadMissingVerses(readerId, surahId, missingVerses)
                }
                .setNegativeButton("الوضع المتصل") { _, _ ->
                    switchToOnlineMode()
                }
                .setNeutralButton("إلغاء") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showNoNetworkDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("لا يوجد اتصال بالإنترنت")
            .setMessage("المحتوى غير متوفر للوضع غير المتصل ولا يوجد اتصال بالإنترنت لتحميله.")
            .setPositiveButton("إعادة المحاولة") { _, _ ->
                setupOfflineAudio()
            }
            .setNegativeButton("إغلاق") { _, _ ->
                finish()
            }
            .show()
    }

    private fun extractReaderIdFromIntent(): String? {
        return intent.getStringExtra("readerId")
    }

    private fun getSurahName(surahId: Int): String {
        return Constants.SORA_OF_QURAN.getOrElse(surahId) { "Surah $surahId" }
    }

    private fun getOnlineBaseUrl(readerId: String?): String {
        val normalizedReaderId = readerId?.let { normalizeToAsciiDigits(it) } ?: ""
        return "https://verse.mp3quran.net/arabic/reader_$normalizedReaderId/128"
    }

    private fun normalizeToAsciiDigits(input: String): String {
        return input.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }

    private fun setupProgressObservers() {
        lifecycleScope.launch {
            memorizationViewModel.uiState.collect { state ->
                if (state.message != null) {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    memorizationViewModel.dismissMessage()
                }
                if (state.error != null) {
                    Snackbar.make(binding.root, state.error, Snackbar.LENGTH_LONG).show()
                    memorizationViewModel.dismissError()
                }
                if (state.showCelebration) {
                    showCelebrationDialog()
                    memorizationViewModel.dismissCelebration()
                }
            }
        }
    }

    private fun checkProgressTrackingEligibility() {
        lifecycleScope.launch {
            memorizationViewModel.todayTarget.collect { todayTarget ->
                todayTarget?.let { target ->
                    val sessionSurahId = soraId?.toIntOrNull() ?: -1
                    val sessionStartVerse = startAya?.toIntOrNull() ?: -1
                    val sessionEndVerse = endAya?.toIntOrNull() ?: -1

                    targetStartVerse = target.startVerse
                    targetEndVerse = target.endVerse
                    this@HefzRepeatActivity.sessionStartVerse = sessionStartVerse
                    this@HefzRepeatActivity.sessionEndVerse = sessionEndVerse

                    val hasOverlap = sessionSurahId == target.surahId &&
                            sessionStartVerse <= target.endVerse &&
                            sessionEndVerse >= target.startVerse &&
                            !target.isCompleted

                    isProgressTrackingEnabled = hasOverlap

                    if (isProgressTrackingEnabled) {
                        showProgressTrackingConfirmation(
                            target.surahName,
                            sessionStartVerse,
                            sessionEndVerse,
                            target
                        )
                    } else if (isOfflineMode) {
                        binding.toolbar.toolbar.subtitle = getString(R.string.offline_mode_active)
                    }
                }
            }
        }
    }


    private fun showProgressTrackingConfirmation(
        surahName: String,
        startVerse: Int,
        endVerse: Int,
        target: DailyTarget,
    ) {
        if (isDialogShowing || completionDialogShown) return

        val overlapStart = maxOf(sessionStartVerse, target.startVerse)
        val overlapEnd = minOf(sessionEndVerse, target.endVerse)

        val modeText = if (isOfflineMode) "\n\n(سيتم الحفظ في وضع عدم الاتصال)" else ""
        val message = """
            هدف اليوم: ${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}
            الجلسة الحالية: الآيات $sessionStartVerse-$sessionEndVerse
            المنطقة المشتركة: الآيات $overlapStart-$overlapEnd
            التقدم الحالي: ${target.completedVerses}/${target.getTotalVerses()} آية
            
            كيف تريد التعامل مع إكمال الهدف؟$modeText
        """.trimIndent()

        isDialogShowing = true
        completionDialogShown = true
        MaterialAlertDialogBuilder(this)
            .setTitle("إعدادات تتبع التقدم")
            .setMessage(message)
            .setPositiveButton("اكتمال تلقائي") { _, _ ->
                isDialogShowing = false
                userChoiceMade = true
                userWantsAutoComplete = true
                startProgressTracking(target)
            }
            .setNeutralButton("حفظ التقدم فقط") { _, _ ->
                isDialogShowing = false
                userChoiceMade = true
                userWantsAutoComplete = false
                startProgressTracking(target)
            }
            .setNegativeButton("بدون تتبع") { _, _ ->
                isDialogShowing = false
                isProgressTrackingEnabled = false
                binding.toolbar.toolbar.subtitle =
                    if (isOfflineMode) getString(R.string.offline_mode_active) else ""
            }
            .setCancelable(false)
            .show()
    }

    private fun startProgressTracking(target: DailyTarget) {
        lifecycleScope.launch {
            try {
                sessionStartTime = Date()
                totalVersesInTarget = target.getTotalVerses()

                val currentProgress =
                    memorizationViewModel.getCurrentProgress() ?: target.completedVerses
                initialCompletedVerses = currentProgress
                sessionStartProgress = currentProgress
                accumulatedVersesCompleted = currentProgress

                versesCompleted = 0
                shouldSaveProgress = true

                memorizationViewModel.startMemorizationSession(SessionType.LISTENING)
                memorizationViewModel.uiState.collect { state ->
                    if (state.isSessionActive && state.currentSessionId != null) {
                        sessionId = state.currentSessionId
                        updateProgressDisplay()
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "فشل في بدء تتبع التقدم: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
                isProgressTrackingEnabled = false
            }
        }
    }

    private fun showCelebrationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.congratulations_title))
            .setMessage(getString(R.string.congratulations_message))
            .setPositiveButton(getString(R.string.thanks)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showExitConfirmationDialog() {
        if (isDialogShowing) return

        val progressInThisSession = accumulatedVersesCompleted - initialCompletedVerses
        val hasProgress = progressInThisSession > 0

        if (!hasProgress) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.end_session))
                .setMessage("لم يتم تسجيل أي تقدم في هذه الجلسة.\n\nهل تريد الخروج؟")
                .setPositiveButton("خروج") { _, _ ->
                    stopMemorization()
                    finish()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        val message =
            "تقدمك في هذه الجلسة: $progressInThisSession آية\nالتقدم الإجمالي: $accumulatedVersesCompleted/$totalVersesInTarget آية\n\nهل تريد حفظ التقدم قبل الخروج؟"

        isDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.end_session))
            .setMessage(message)
            .setPositiveButton("حفظ والخروج") { _, _ ->
                isDialogShowing = false
                shouldSaveProgress = true
                isExitingWithoutSave = false

                saveProgressAndExit()
            }
            .setNeutralButton("خروج بدون حفظ") { _, _ ->
                isDialogShowing = false
                shouldSaveProgress = false
                isExitingWithoutSave = true

                cancelProgressAndExit()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                isDialogShowing = false
                dialog.dismiss()
            }
            .show()

    }
    private fun saveProgressAndExit() {
        lifecycleScope.launch {
            try {
                binding.toolbar.toolbar.subtitle = "جاري حفظ التقدم..."

                progressUpdateMutex.withLock {
                    if (pendingProgressUpdates.isNotEmpty()) {
                        val finalProgress = pendingProgressUpdates.last()
                        memorizationViewModel.updateVerseProgress(finalProgress)

                        delay(500)
                    }
                }

                completeProgressTracking(markAsCompleted = false, forceComplete = false)

                delay(300)

                stopMemorization()
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "فشل في حفظ التقدم: ${e.message}", Snackbar.LENGTH_LONG).show()
                stopMemorization()
                finish()
            }
        }
    }

    private fun cancelProgressAndExit() {
        lifecycleScope.launch {
            try {
                binding.toolbar.toolbar.subtitle = "جاري إلغاء التغييرات..."
                progressUpdateMutex.withLock {
                    pendingProgressUpdates.clear()

                    if (isProgressTrackingEnabled && sessionId != null) {
                        memorizationViewModel.updateVerseProgress(initialCompletedVerses)

                        memorizationViewModel.completeSession(
                            versesCompleted = initialCompletedVerses,
                            notes = "جلسة ملغية - لم يتم حفظ التقدم",
                            markAsCompleted = false
                        )

                        delay(500)
                    }
                }

                stopMemorization()
                finish()
            } catch (_: Exception) {
                stopMemorization()
                finish()
            }
        }
    }


    private fun restoreOriginalProgress() {
        if (isProgressTrackingEnabled && sessionId != null) {
            lifecycleScope.launch {
                try {
                    memorizationViewModel.updateVerseProgress(initialCompletedVerses)

                    memorizationViewModel.completeSession(
                        versesCompleted = initialCompletedVerses,
                        notes = "جلسة ملغية - لم يتم حفظ التقدم",
                        markAsCompleted = false
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun showManualCompletionDialog() {
        if (isDialogShowing) return

        val progressInThisSession = accumulatedVersesCompleted - initialCompletedVerses

        isDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle("إنهاء الجلسة")
            .setMessage(
                """
            تقدمك في هذه الجلسة: $progressInThisSession آية
            التقدم الإجمالي: $accumulatedVersesCompleted/$totalVersesInTarget آية
            
            كيف تريد إنهاء هذه الجلسة؟
        """.trimIndent()
            )
            .setPositiveButton("إكمال الهدف") { _, _ ->
                isDialogShowing = false
                completeProgressTracking(markAsCompleted = true, forceComplete = true)
                stopMemorization()
            }
            .setNeutralButton("حفظ التقدم فقط") { _, _ ->
                isDialogShowing = false
                completeProgressTracking(markAsCompleted = false, forceComplete = false)
                stopMemorization()
            }
            .setNegativeButton("إلغاء") { dialog, _ ->
                isDialogShowing = false
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        if (isProgressTrackingEnabled && sessionId != null && shouldSaveProgress) {
            completeProgressTracking(markAsCompleted = false, forceComplete = false)
        } else if (isProgressTrackingEnabled && sessionId != null) {
            restoreOriginalProgress()
        }

        stopMemorization()
        exoPlayer?.release()
        exoPlayer = null

        isDialogShowing = false
        userChoiceMade = false
        userWantsAutoComplete = false
        completionDialogShown = false

        lifecycleScope.cancel()
        super.onDestroy()
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

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })
    }

    private fun onVerseCompleted(versePosition: Int) {
        if (isProgressTrackingEnabled && userChoiceMade && !isExitingWithoutSave) {
            val currentVerseNumber = sessionStartVerse + versePosition
            val resumeFromVerseNumber = targetStartVerse + initialCompletedVerses

            if (currentVerseNumber >= resumeFromVerseNumber) {
                val progressBeyondResumePoint = currentVerseNumber - resumeFromVerseNumber + 1
                val newAccumulatedProgress = initialCompletedVerses + progressBeyondResumePoint

                if (newAccumulatedProgress <= totalVersesInTarget) {
                    accumulatedVersesCompleted = newAccumulatedProgress
                    versesCompleted = progressBeyondResumePoint
                    updateProgressDisplay()

                    if (shouldSaveProgress && !isExitingWithoutSave) {
                        saveProgressSilently()
                        checkForAutoCompletion()
                    }
                }
            }
        }
    }



    private fun checkForAutoCompletion() {
        if (!isProgressTrackingEnabled || !userChoiceMade) return

        if (accumulatedVersesCompleted >= totalVersesInTarget) {
            if (!completionDialogShown) {
                completionDialogShown = true
                if (userWantsAutoComplete) {
                    lifecycleScope.launch {
                        completeProgressTracking(true, true)
                        showCompletionToast("تهانينا! تم إكمال الهدف بنجاح!")
                    }
                } else {
                    lifecycleScope.launch {
                        saveProgressSilently()
                        showCompletionToast("تم حفظ جميع الآيات المطلوبة")
                    }
                }
            }
        }
    }

    private fun showCompletionToast(message: String) {
        if (!completionDialogShown) {
            completionDialogShown = true
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.green))
                .show()
        }
    }

    private fun checkForCompletion() {
        if (isProgressTrackingEnabled && !hasShownCompletionDialog) {
            val targetOverlapStart = maxOf(sessionStartVerse, targetStartVerse)
            val targetOverlapEnd = minOf(sessionEndVerse, targetEndVerse)
            val sessionVerseNumber = sessionStartVerse + versesCompleted - 1

            if (sessionVerseNumber >= targetOverlapEnd) {
                val newCompletedInTarget =
                    initialCompletedVerses + (targetOverlapEnd - targetOverlapStart + 1)
                if (newCompletedInTarget >= totalVersesInTarget) {
                    hasShownCompletionDialog = true
                    showAutoCompletionDialog()
                }
            }
        }
    }

    private fun saveProgressSilently() {
        if (!shouldSaveProgress || isExitingWithoutSave) return

        if (accumulatedVersesCompleted > initialCompletedVerses) {
            lifecycleScope.launch {
                progressUpdateMutex.withLock {
                    try {
                        pendingProgressUpdates.add(accumulatedVersesCompleted)

                        if (!isExitingWithoutSave && shouldSaveProgress) {
                            memorizationViewModel.updateVerseProgress(accumulatedVersesCompleted)
                            lastSavedProgress = accumulatedVersesCompleted
                            delay(100)
                        }
                    } catch (_: Exception) {

                        pendingProgressUpdates.remove(accumulatedVersesCompleted)
                    }
                }
            }
        }
    }

    private fun updateProgressDisplay() {
        if (isProgressTrackingEnabled) {
            val percentageProgress = if (totalVersesInTarget > 0) {
                (accumulatedVersesCompleted * 100) / totalVersesInTarget
            } else 0

            binding.toolbar.toolbar.subtitle =
                "تتبع التقدم: $accumulatedVersesCompleted/$totalVersesInTarget آية ($percentageProgress%)"
        }
    }


    private fun setupClickListeners() {
        binding.cancel.setOnClickListener {
            if (isProgressTrackingEnabled && sessionId != null) {
                showExitConfirmationDialog()
            } else {
                stopMemorization()
                finish()
            }
        }

        binding.stopMemorization.setOnClickListener {
            if (isProgressTrackingEnabled && sessionId != null) {
                showManualCompletionDialog()
            } else {
                stopMemorization()
            }
        }

        binding.pauseResumeButton.setOnClickListener { togglePauseResume() }
        binding.skipForwardButton.setOnClickListener { skipToNextVerse() }
    }

    private fun showCompletionConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.complete_session))
            .setMessage(
                """
                تقدمك الحالي: $versesCompleted من $totalVersesInTarget آية
                
                اختر إحدى الخيارات:
                • مكتمل: سجل الهدف كمكتمل بالكامل
                • حفظ التقدم: احفظ التقدم الحالي فقط
                • إلغاء: استمر في الجلسة
            """.trimIndent()
            )
            .setPositiveButton("مكتمل") { _, _ ->
                completeProgressTracking(markAsCompleted = true, forceComplete = true)
                stopMemorization()
            }
            .setNeutralButton("حفظ التقدم") { _, _ ->
                completeProgressTracking(markAsCompleted = false, forceComplete = false)
                stopMemorization()
            }
            .setNegativeButton("إلغاء") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun completeProgressTracking(markAsCompleted: Boolean, forceComplete: Boolean = false) {
        lifecycleScope.launch {
            progressUpdateMutex.withLock {
                try {
                    sessionId?.let { id ->
                        val finalProgress = if (shouldSaveProgress && !isExitingWithoutSave) {
                            accumulatedVersesCompleted
                        } else {
                            initialCompletedVerses
                        }

                        val notes = when {
                            isExitingWithoutSave -> "جلسة ملغية - لم يتم حفظ التقدم"
                            forceComplete -> "هدف مكتمل - تم وضع علامة اكتمال يدوياً"
                            markAsCompleted -> "جلسة مكتملة - المجموع: $finalProgress من $totalVersesInTarget آية"
                            else -> "تقدم محفوظ - المجموع: $finalProgress من $totalVersesInTarget آية"
                        }

                        memorizationViewModel.completeSession(
                            versesCompleted = finalProgress,
                            notes = notes,
                            markAsCompleted = shouldSaveProgress && !isExitingWithoutSave && (forceComplete || accumulatedVersesCompleted >= totalVersesInTarget)
                        )

                        delay(200)
                    }
                    isProgressTrackingEnabled = false
                    sessionId = null
                } catch (e: Exception) {
                    if (shouldSaveProgress && !isExitingWithoutSave) {
                        Snackbar.make(binding.root, "فشل في حفظ التقدم: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    private fun checkAutoCompletion() {
        if (isProgressTrackingEnabled && versesCompleted >= totalVersesInTarget) {
            showAutoCompletionDialog()
        }
    }

    private fun showAutoCompletionDialog() {
        if (isDialogShowing || hasShownCompletionDialog) return

        isDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle("تم إكمال الهدف!")
            .setMessage("تهانينا! لقد أكملت هدف اليوم بنجاح.\n\nهل تريد تسجيل هذا الهدف كمكتمل؟")
            .setPositiveButton("نعم، مكتمل") { _, _ ->
                isDialogShowing = false
                completeProgressTracking(markAsCompleted = true, forceComplete = true)
            }
            .setNegativeButton("حفظ التقدم فقط") { _, _ ->
                isDialogShowing = false
                completeProgressTracking(markAsCompleted = false, forceComplete = false)
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun awaitAyaCompletion() {
        exoPlayer?.let { player ->
            var waitCount = 0
            val maxWaitCount = 500

            while (player.playbackState != Player.STATE_READY &&
                player.playbackState != Player.STATE_BUFFERING &&
                waitCount < maxWaitCount &&
                !isMemorizationStopped &&
                !skipToNext
            ) {
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
                !skipToNext
            ) {
                while (isMemorizationPaused && !isMemorizationStopped && !skipToNext) {
                    delay(100)
                }
                if (isMemorizationStopped || skipToNext) break
                delay(100)
                waitCount++
            }

            if (player.playbackState == Player.STATE_ENDED || skipToNext) {
                val currentPosition = binding.viewPager.currentItem
                onVerseCompleted(currentPosition) // Count completion at the END
                player.stop()
            }
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
        versesCompleted = 0
        updateProgressDisplay()
        startMemorizationProcess()
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
                if (isProgressTrackingEnabled && sessionId != null) {
                    showExitConfirmationDialog()
                } else {
                    stopMemorization()
                    finish()
                }
                true
            }

            else -> false
        }
    }
}