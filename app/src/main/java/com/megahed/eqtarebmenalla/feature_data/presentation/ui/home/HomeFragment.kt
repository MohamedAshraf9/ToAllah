package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.navigation.NavigationView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.alarm.Constants
import com.megahed.eqtarebmenalla.alarm.NotifyMessing
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.FragmentHomeBinding
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.settings.SettingsActivity
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import com.megahed.eqtarebmenalla.worker.PrayerTimesScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.db.model.UserStreak
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import com.megahed.eqtarebmenalla.feature_data.states.MemorizationUiState
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import androidx.core.view.isGone
import com.megahed.eqtarebmenalla.db.model.getTotalVerses
import com.megahed.eqtarebmenalla.feature_data.data.repository.DailyTargetProgress

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
        private const val PREF_LATITUDE = "saved_latitude"
        private const val PREF_LONGITUDE = "saved_longitude"
        private const val PREF_LOCATION_NAME = "saved_location_name"
        private const val PREF_LOCATION_SAVED = "location_saved"
    }

    private var prayerTimeAlarm: PrayerTime = PrayerTime()
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private val mainViewModel: IslamicViewModel by activityViewModels()

    private val memorizationViewModel: MemorizationViewModel by viewModels()

    private lateinit var binding: FragmentHomeBinding

    private var countDownTimer: CountDownTimer? = null
    private var timeStarted: Long = 0
    private var timeElapsed: Long = 0
    private var isUpdatingLocation = false
    private var isLocationUpdateRequested = false

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedTimeRunnable: Runnable? = null

    override fun onStart() {
        super.onStart()
        memorizationViewModel.refreshAllData()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val prayerTimeViewModel = ViewModelProvider(this)[PrayerTimeViewModel::class.java]
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        sharedPreference = requireActivity().getSharedPreferences("adhen", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupLocationRequest()

        if (!sharedPreference.getBoolean("firstTime", false)) {
            editor.putBoolean("firstTime", true)
            editor.apply()
        }

        binding.dayDetails.text = DateFormat.getDateInstance(DateFormat.FULL).format(Date())

        loadSavedLocationOrRequest()

        setupViewModelObservers(prayerTimeViewModel)

        setupMemorizationObservers()

        setupPrayerNotificationCheckboxes(editor)

        setupClickListeners(drawerLayout, navView)

        setupMemorizationClickListeners()

        return root
    }

    private fun setupMemorizationObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            memorizationViewModel.currentSchedule.collectLatest { schedule ->
                updateMemorizationScheduleUI(schedule)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memorizationViewModel.todayTarget.collectLatest { target ->
                if (target != null) {
                    updateTodayProgressText(target)
                    memorizationViewModel.loadTodayTargetProgress()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memorizationViewModel.todayTargetProgress.collectLatest { targetProgress ->
                if (targetProgress != null) {
                    updateTodayProgressText(targetProgress.target)
                    updateTodayTargetUI(targetProgress.target, targetProgress)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memorizationViewModel.userStreak.collectLatest { streak ->
                updateStreakUI(streak)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            memorizationViewModel.uiState.collectLatest { state ->
                handleMemorizationUIState(state)
            }
        }
    }
    private fun updateTodayProgressText(target: DailyTarget) {
        val totalVerses = target.getTotalVerses()
        val completedText = "${target.completedVerses}/$totalVerses آيات"
        binding.todayProgressText.text = completedText

        when {
            target.isCompleted -> {
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }
            target.completedVerses > 0 -> {
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }
            else -> {
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    private fun showCelebrationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.congratulations_title))
            .setMessage(getString(R.string.celebration_message))
            .setPositiveButton(getString(R.string.alhamdulillah)) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_celebration)
            .show()
    }

    private fun showScheduleDetails() {
        val schedule = memorizationViewModel.currentSchedule.value
        if (schedule != null) {
            showScheduleDetailsDialog(schedule)
        } else {
            Snackbar.make(
                binding.root,
                getString(R.string.no_active_schedule),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showQuickProgressDialog() {
        val schedule = memorizationViewModel.currentSchedule.value
        val progress = memorizationViewModel.uiState.value.scheduleProgress
        val streak = memorizationViewModel.userStreak.value
        val todayProgress = memorizationViewModel.todayTargetProgress.value

        if (schedule != null && progress != null) {
            val todayProgressText = todayProgress?.let {
                "\n📖 تقدم اليوم: ${it.completedVerses}/${it.totalVerses} آية (${it.progressPercentage}%)"
            } ?: ""

            val message = """
            📚 الجدول: ${schedule.title}
            
            📊 التقدم الإجمالي: ${progress.progressPercentage}%
            ✅ الأهداف المكتملة: ${progress.completedTargets}/${progress.totalTargets}
            📅 الأيام المتبقية: ${calculateDaysRemaining(schedule.endDate)} يوم$todayProgressText
            
            🔥 السلسلة الحالية: ${streak?.currentStreak ?: 0} أيام
            🏆 أفضل سلسلة: ${streak?.longestStreak ?: 0} أيام
        """.trimIndent()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.progress_summary))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .setNeutralButton(getString(R.string.view_details)) { _, _ ->
                    showScheduleDetails()
                }
                .show()
        } else {
            Snackbar.make(binding.root, getString(R.string.no_progress_data), Snackbar.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMemorizationScheduleUI(schedule: MemorizationSchedule?) {
        if (schedule != null) {
            binding.memorizationWidget.visibility = View.VISIBLE
            memorizationViewModel.loadScheduleProgress()

            val timeRemaining = calculateTimeRemaining(schedule.endDate)
            binding.scheduleSubtitle.text = "${schedule.title} - $timeRemaining"
        } else {
            showMemorizationEmptyState()
        }
    }


    private fun calculateTimeRemaining(endDate: Date): String {
        val currentTime = System.currentTimeMillis()
        val endTime = endDate.time
        val timeDifference = endTime - currentTime

        return when {
            timeDifference <= 0 -> "منتهي"
            timeDifference < 24 * 60 * 60 * 1000 -> {
                formatCountdown(timeDifference)
            }

            else -> {
                val daysRemaining = (timeDifference / (24 * 60 * 60 * 1000)).toInt()
                val hoursRemaining =
                    ((timeDifference % (24 * 60 * 60 * 1000)) / (1000 * 60 * 60)).toInt()
                "متبقي $daysRemaining يوم و $hoursRemaining ساعة"
            }
        }
    }

    private fun formatCountdown(milliseconds: Long): String {
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = ((milliseconds % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val seconds = ((milliseconds % (1000 * 60)) / 1000).toInt()

        return when {
            hours > 0 -> "متبقي ${hours}س ${minutes}د"
            minutes > 0 -> "متبقي ${minutes}د ${seconds}ث"
            else -> "متبقي ${seconds}ث"
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateTodayTargetUI(target: DailyTarget?, targetProgress: DailyTargetProgress?) {
        if (target != null && targetProgress != null) {
            binding.activeScheduleLayout.visibility = View.VISIBLE
            binding.noScheduleLayout.visibility = View.GONE

            val targetInfo = "${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}"
            binding.activeScheduleTitle.text = targetInfo

            // Show partial progress in verse count
            val progressText = "${targetProgress.completedVerses}/${targetProgress.totalVerses} آية"
            binding.todayProgressText.text = progressText

            binding.estimatedTime.text = "الوقت المقدر: ${target.estimatedDurationMinutes} دقيقة"

            when {
                target.isCompleted -> {
                    binding.todayCompletionIcon.visibility = View.VISIBLE
                    binding.todayCompletionIcon.setImageResource(R.drawable.ic_check_circle)
                    binding.activeScheduleLayout.setBackgroundResource(R.drawable.memorization_completed_background)
                    binding.todayProgressText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.green)
                    )
                }
                targetProgress.completedVerses > 0 -> {
                    binding.todayCompletionIcon.visibility = View.VISIBLE
                    binding.todayCompletionIcon.setImageResource(R.drawable.ic_partial_progress)
                    binding.todayProgressText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                    )
                }
                else -> {
                    binding.todayCompletionIcon.visibility = View.GONE
                    binding.activeScheduleLayout.setBackgroundResource(R.drawable.today_target_background)
                    binding.todayProgressText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                    )
                }
            }
        } else if (target != null) {

            updateTodayTargetUILegacy(target)
        } else {
            binding.activeScheduleLayout.visibility = View.GONE
            binding.noScheduleLayout.visibility = View.VISIBLE
        }
        updateMemorizationButtons(target, targetProgress)
    }
    @SuppressLint("SetTextI18n")
    private fun updateTodayTargetUILegacy(target: DailyTarget) {
        binding.activeScheduleLayout.visibility = View.VISIBLE
        binding.noScheduleLayout.visibility = View.GONE

        val targetInfo = "${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}"
        binding.activeScheduleTitle.text = targetInfo

        val totalVerses = target.getTotalVerses()
        val progressText = "${target.completedVerses}/$totalVerses آية"
        binding.todayProgressText.text = progressText

        binding.estimatedTime.text = "الوقت المقدر: ${target.estimatedDurationMinutes} دقيقة"

        when {
            target.isCompleted -> {
                binding.todayCompletionIcon.visibility = View.VISIBLE
                binding.todayCompletionIcon.setImageResource(R.drawable.ic_check_circle)
                binding.activeScheduleLayout.setBackgroundResource(R.drawable.memorization_completed_background)
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }
            target.completedVerses > 0 -> {
                binding.todayCompletionIcon.visibility = View.VISIBLE
                binding.todayCompletionIcon.setImageResource(R.drawable.ic_partial_progress)
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }
            else -> {
                binding.todayCompletionIcon.visibility = View.GONE
                binding.activeScheduleLayout.setBackgroundResource(R.drawable.today_target_background)
                binding.todayProgressText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }
        }
    }

    private fun updateMemorizationButtons(target: DailyTarget?, targetProgress: DailyTargetProgress?) {
        val primaryButton = binding.btnPrimaryAction
        val secondaryButton = binding.btnSecondaryAction

        if (target != null) {
            when {
                target.isCompleted -> {
                    primaryButton.text = getString(R.string.completed)
                    primaryButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
                    primaryButton.isEnabled = false
                    secondaryButton.text = getString(R.string.review)
                    secondaryButton.visibility = View.VISIBLE
                }
                targetProgress != null && targetProgress.completedVerses > 0 -> {
                    primaryButton.text = "متابعة الحفظ"
                    primaryButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_circle)
                    primaryButton.isEnabled = true
                    secondaryButton.text = getString(R.string.view_schedule_details)
                    secondaryButton.visibility = View.VISIBLE
                }
                else -> {
                    primaryButton.text = getString(R.string.start_memorization)
                    primaryButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_circle)
                    primaryButton.isEnabled = true
                    secondaryButton.text = getString(R.string.view_schedule_details)
                    secondaryButton.visibility = View.VISIBLE
                }
            }
        } else {
            primaryButton.text = getString(R.string.create_schedule)
            primaryButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add_circle)
            primaryButton.isEnabled = true
            secondaryButton.visibility = View.GONE
        }
    }


    private fun updateStreakUI(streak: UserStreak?) {
        if (streak != null && streak.currentStreak > 0) {
            binding.streakContainer.visibility = View.VISIBLE
            binding.memorizationStreakBadge.text = "${streak.currentStreak} أيام"

            when {
                streak.currentStreak >= 50 -> {
                    binding.streakContainer.setBackgroundResource(R.drawable.streak_gold_background)
                }

                streak.currentStreak >= 14 -> {
                    binding.streakContainer.setBackgroundResource(R.drawable.streak_silver_background)
                }

                else -> {
                    binding.streakContainer.setBackgroundResource(R.drawable.streak_container_background)
                }
            }
        } else {
            binding.streakContainer.visibility = View.GONE
        }
    }

    private fun handleMemorizationUIState(state: MemorizationUiState) {
        if (state.isLoading) {

        }

        state.message?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                .show()
            memorizationViewModel.dismissMessage()

            memorizationViewModel.loadTodayTargetProgress()
        }

        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red))
                .show()
            memorizationViewModel.dismissError()
        }

        if (state.showCelebration) {
            showCelebrationDialog()
            memorizationViewModel.dismissCelebration()

            memorizationViewModel.loadTodayTargetProgress()
        }

        state.scheduleProgress?.let { progress ->
            binding.totalProgressText.text = "${progress.progressPercentage}%"
        }

        val target = memorizationViewModel.todayTarget.value
        if (target != null) {
            updateTodayProgressText(target)
        }

        val streak = memorizationViewModel.userStreak.value
        binding.currentStreakText.text = "${streak?.currentStreak ?: 0} أيام"
    }

    private fun setupMemorizationClickListeners() {
        binding.btnPrimaryAction.setOnClickListener {
            when (binding.btnPrimaryAction.text.toString()) {
                "بدء الحفظ", "متابعة الحفظ" -> startMemorizationSession()
                "إنشاء جدول", "إنشاء جدول حفظ" -> navigateToScheduleCreation()
                "مراجعة" -> startReviewSession()
            }
        }

        binding.btnSecondaryAction.setOnClickListener {
            when (binding.btnSecondaryAction.text.toString()) {
                "عرض التفاصيل" -> showScheduleDetails()
                "مراجعة" -> startReviewSession()
            }
        }

        binding.expandCollapseContainer.setOnClickListener {
            toggleQuickStats()
        }

        binding.progressOverview.setOnClickListener {
            showQuickProgressDialog()
        }

        binding.todayCompletionIcon.setOnClickListener {
            val target = memorizationViewModel.todayTarget.value
            if (target != null && !target.isCompleted) {
                showMarkCompletedDialog(target)
            }
        }
    }

    private fun showMemorizationEmptyState() {
        binding.memorizationWidget.visibility = View.VISIBLE
        binding.activeScheduleLayout.visibility = View.GONE
        binding.noScheduleLayout.visibility = View.VISIBLE
        binding.streakContainer.visibility = View.GONE

        binding.todayProgressText.text = "0/0 آيات"
        binding.totalProgressText.text = "0%"
        binding.currentStreakText.text = "0 أيام"
    }

    private fun loadQuickStats() {
        lifecycleScope.launch {
            val weeklyStats = memorizationViewModel.getWeeklyStats()
            val streak = memorizationViewModel.userStreak.value

            binding.thisWeekCount.text = weeklyStats.completedDays.toString()
            binding.totalSessionsCount.text = weeklyStats.targetsCompleted.toString()
            binding.bestStreakCount.text = (streak?.longestStreak ?: 0).toString()
        }
    }

    private fun toggleQuickStats() {
        val statsLayout = binding.quickStatsLayout
        val expandIcon = binding.expandCollapseIcon
        val expandText = binding.expandCollapseText

        if (statsLayout.isGone) {
            statsLayout.visibility = View.VISIBLE
            expandIcon.animate()?.rotation(180f)?.setDuration(300)?.start()
            expandText.text = getString(R.string.hide_stats)
            loadQuickStats()
        } else {
            statsLayout.visibility = View.GONE
            expandIcon.animate()?.rotation(0f)?.setDuration(300)?.start()
            expandText.text = getString(R.string.show_stats)
        }
    }

    private fun showScheduleDetailsDialog(schedule: MemorizationSchedule) {
        val remainingDays = calculateDaysRemaining(schedule.endDate)
        val progress = memorizationViewModel.uiState.value.scheduleProgress

        val message = """
            📅 تاريخ البدء: ${formatDate(schedule.startDate)}
            📅 تاريخ الانتهاء: ${formatDate(schedule.endDate)}
            ⏰ الأيام المتبقية: $remainingDays يوم
            📊 التقدم: ${progress?.progressPercentage ?: 0}%
            ✅ الأهداف المكتملة: ${progress?.completedTargets ?: 0}/${progress?.totalTargets ?: 0}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(schedule.title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(getString(R.string.edit_schedule)) { _, _ ->
                findNavController().navigate(R.id.action_navigation_home_to_scheduleCreationFragment)
            }
            .show()
    }

    private fun showMarkCompletedDialog(target: DailyTarget) {
        val currentProgress = "${target.completedVerses}/${target.getTotalVerses()}"
        val message = if (target.completedVerses > 0) {
            "التقدم الحالي: $currentProgress آية\n\nهل أكملت حفظ ${target.surahName} - الآيات ${target.startVerse}-${target.endVerse} فعلاً؟"
        } else {
            "هل أكملت حفظ ${target.surahName} - الآيات ${target.startVerse}-${target.endVerse} فعلاً؟"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_completion))
            .setMessage(message)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                memorizationViewModel.markTodayTargetCompleted()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun calculateDaysRemaining(endDate: Date): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val diffInMillis = endDate.time - today.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }


    private fun startMemorizationSession() {
        val target = memorizationViewModel.todayTarget.value
        if (target != null) {
            val bundle = Bundle().apply {
                putInt("surahId", target.surahId)
                putString("surahName", target.surahName)
                putInt("startVerse", target.startVerse)
                putInt("endVerse", target.endVerse)
                putBoolean("isMemorizationMode", true)
                putBoolean("autoFill", true)
            }
            findNavController().navigate(R.id.action_navigation_home_to_listenerHelperFragment2, bundle)
        } else {
            findNavController().navigate(R.id.action_navigation_home_to_scheduleCreationFragment)
        }
    }

    private fun navigateToScheduleCreation() {
        findNavController().navigate(R.id.action_navigation_home_to_scheduleCreationFragment)
    }

    private fun startReviewSession() {
        val target = memorizationViewModel.todayTarget.value
        if (target != null) {
            val bundle = Bundle().apply {
                putInt("surahId", target.surahId)
                putString("surahName", target.surahName)
                putInt("startVerse", target.startVerse)
                putInt("endVerse", target.endVerse)
                putBoolean("isReviewMode", true)
            }
            findNavController().navigate(
                R.id.action_navigation_home_to_listenerHelperFragment2,
                bundle
            )
        }
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(30000)
            .setMaxUpdateDelayMillis(60000)
            .build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d(
                        "HomeFragment",
                        "Location received: ${location.latitude}, ${location.longitude}"
                    )
                    handleLocationUpdate(location)
                    stopLocationUpdates()
                    break
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        if (::mLocationCallback.isInitialized) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            isUpdatingLocation = false
            isLocationUpdateRequested = false
            Log.d("HomeFragment", "Location updates stopped")
        }
    }

    private fun loadSavedLocationOrRequest() {
        if (!isLocationUpdateRequested && sharedPreference.getBoolean(PREF_LOCATION_SAVED, false)) {
            val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
            val savedLongitude = sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()
            val savedLocationName =
                sharedPreference.getString(PREF_LOCATION_NAME, "Unknown Location")

            binding.currentLocation.text = savedLocationName

            if (MethodHelper.isOnline(requireContext())) {
                mainViewModel.getAzanData(savedLatitude, savedLongitude)
            }

            Log.d("HomeFragment", "Using saved location: $savedLatitude, $savedLongitude")
        } else {
            requestLocationUpdate()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun requestLocationUpdate() {
        isUpdatingLocation = true
        isLocationUpdateRequested = true
        binding.currentLocation.text = "Updating location..."
        statusLocationCheck()
    }

    private fun handleLocationUpdate(location: Location) {
        val editor = sharedPreference.edit()

        editor.putFloat(PREF_LATITUDE, location.latitude.toFloat())
        editor.putFloat(PREF_LONGITUDE, location.longitude.toFloat())
        editor.putBoolean(PREF_LOCATION_SAVED, true)

        getCountryFromLocation(location) { locationName ->

            editor.putString(PREF_LOCATION_NAME, locationName)
            editor.apply()

            binding.currentLocation.text = locationName
            Log.d("HomeFragment", "Location name saved: $locationName")
        }

        setPrayerTimesUpdateWorker(location)

        if (MethodHelper.isOnline(requireContext())) {
            mainViewModel.getAzanData(location.latitude, location.longitude)
        } else {
            if (isLocationUpdateRequested) {
                showNoInternetWarning()
            }
        }

        isUpdatingLocation = false
        isLocationUpdateRequested = false
        Log.d(
            "HomeFragment",
            "Location updated and saved: ${location.latitude}, ${location.longitude}"
        )
    }

    private fun setupViewModelObservers(prayerTimeViewModel: PrayerTimeViewModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.state.collect { islamicListState ->
                    islamicListState.let { islamicInfo ->
                        islamicInfo.islamicInfo.data?.let {

                            val prayerTime = PrayerTime(
                                1,
                                it.date.gregorian.date,
                                it.timings.Asr,
                                it.timings.Dhuhr,
                                it.timings.Fajr,
                                it.timings.Isha,
                                it.timings.Maghrib,
                                it.timings.Sunrise
                            )
                            prayerTimeViewModel.insertPrayerTime(prayerTime)

                            binding.hijriDate.text = formatHijriDate(it.date.hijri.date)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                prayerTimeViewModel.getPrayerTimeById().collect { prayerTime ->
                    prayerTime?.let {
                        prayerTimeAlarm = it
                        updatePrayerTimesUI(it)
                        updateCurrentPrayerStatus(it)

                        if (MethodHelper.isOnline(requireContext()) && !isUpdatingLocation) {
                            updateAzan()
                        }
                    } ?: run {
                        if (!isUpdatingLocation) {
                            showLoadingState()
                        }
                    }
                }
            }
        }
    }

    fun formatHijriDate(hijriDate: String): String {
        val parts = hijriDate.split("-")
        if (parts.size != 3) return hijriDate

        val day = parts[0].toIntOrNull() ?: return hijriDate
        val month = parts[1].toIntOrNull() ?: return hijriDate
        val year = parts[2].toIntOrNull() ?: return hijriDate

        val hijriMonthsEn = arrayOf(
            "Muharram",
            "Safar",
            "Rabi' al-Awwal",
            "Rabi' al-Thani",
            "Jumada al-Awwal",
            "Jumada al-Thani",
            "Rajab",
            "Sha'ban",
            "Ramadan",
            "Shawwal",
            "Dhu al-Qi'dah",
            "Dhu al-Hijjah"
        )

        val hijriMonthsAr = arrayOf(
            "مُحَرَّم",
            "صَفَر",
            "ربيع الأول",
            "ربيع الآخر",
            "جمادى الأولى",
            "جمادى الآخرة",
            "رجب",
            "شعبان",
            "رمضان",
            "شوّال",
            "ذو القعدة",
            "ذو الحجة"
        )

        val isArabic = Locale.getDefault().language == "ar"
        val hijriMonths = if (isArabic) hijriMonthsAr else hijriMonthsEn
        val monthName = if (month in 1..12) hijriMonths[month - 1] else "Unknown"

        val numberFormat = NumberFormat.getInstance(Locale.getDefault())
        numberFormat.isGroupingUsed = false
        return "${numberFormat.format(day)} $monthName ${numberFormat.format(year)}"
    }

    fun formatSalahTimeForLocale(time: String): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            val date = sdf.parse(time) ?: return time

            val cal = Calendar.getInstance()
            cal.time = date

            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            val numberFormat = NumberFormat.getInstance(Locale.getDefault())
            numberFormat.isGroupingUsed = false

            val hourStr = numberFormat.format(hour)
            val minuteStr = String.format(Locale.getDefault(), "%02d", minute)

            "$hourStr:$minuteStr"
        } catch (e: Exception) {
            time
        }
    }


    private fun updatePrayerTimesUI(prayerTime: PrayerTime) {
        binding.fajrTime.text = formatSalahTimeForLocale(prayerTime.Fajr)
        binding.sunriseTime.text = formatSalahTimeForLocale(prayerTime.Sunrise)
        binding.dhuhrTime.text = formatSalahTimeForLocale(prayerTime.Dhuhr)
        binding.asrTime.text = formatSalahTimeForLocale(prayerTime.Asr)
        binding.maghribTime.text = formatSalahTimeForLocale(prayerTime.Maghrib)
        binding.ishaTime.text = formatSalahTimeForLocale(prayerTime.Isha)
    }

    private fun updateCurrentPrayerStatus(prayerTime: PrayerTime) {
        val currentTime = CommonUtils.getCurrentTime()
        val currentTimeLong = CommonUtils.getTimeLong(currentTime, true)

        stopAllTimers()

        when {
            CommonUtils.getTimeLong(prayerTime.Fajr, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.fajr),
                    CommonUtils.convertSalahTime(prayerTime.Fajr),
                    CommonUtils.getTimeLong(prayerTime.Fajr, false) - currentTimeLong
                )
            }

            CommonUtils.getTimeLong(prayerTime.Sunrise, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.sunrise),
                    CommonUtils.convertSalahTime(prayerTime.Sunrise),
                    CommonUtils.getTimeLong(prayerTime.Sunrise, false) - currentTimeLong
                )
            }

            CommonUtils.getTimeLong(prayerTime.Dhuhr, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.duhr),
                    CommonUtils.convertSalahTime(prayerTime.Dhuhr),
                    CommonUtils.getTimeLong(prayerTime.Dhuhr, false) - currentTimeLong
                )
            }

            CommonUtils.getTimeLong(prayerTime.Asr, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.asr),
                    CommonUtils.convertSalahTime(prayerTime.Asr),
                    CommonUtils.getTimeLong(prayerTime.Asr, false) - currentTimeLong
                )
            }

            CommonUtils.getTimeLong(prayerTime.Maghrib, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.maghreb),
                    CommonUtils.convertSalahTime(prayerTime.Maghrib),
                    CommonUtils.getTimeLong(prayerTime.Maghrib, false) - currentTimeLong
                )
            }

            CommonUtils.getTimeLong(prayerTime.Isha, false) >= currentTimeLong -> {
                setDataViewWithCountdown(
                    getString(R.string.isha),
                    CommonUtils.convertSalahTime(prayerTime.Isha),
                    CommonUtils.getTimeLong(prayerTime.Isha, false) - currentTimeLong
                )
            }

            else -> {
                showCountdownToNextDayFajr(prayerTime)
            }
        }
    }

    private fun showCountdownToNextDayFajr(prayerTime: PrayerTime) {
        binding.salahName.text = getString(R.string.fajr)

        val nextDayFajrTimeMillis = getNextDayFajrTime(prayerTime.Fajr)
        val currentTimeMillis = System.currentTimeMillis()
        val timeRemaining = nextDayFajrTimeMillis - currentTimeMillis

        binding.prayerTime.text = formatSalahTimeForLocale(prayerTime.Fajr)

        if (timeRemaining > 0) {
            countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    binding.prayerCountdown.text =
                        "- ${CommonUtils.updateCountDownText(millisUntilFinished)}"
                }

                @SuppressLint("SetTextI18n")
                override fun onFinish() {
                    binding.prayerCountdown.text = "- 00:00:00"
                    if (MethodHelper.isOnline(requireContext())) {
                        val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
                        val savedLongitude =
                            sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()
                        if (savedLatitude != 0.0 && savedLongitude != 0.0) {
                            mainViewModel.getAzanData(savedLatitude, savedLongitude)
                        }
                    }
                }
            }
            countDownTimer?.start()
        } else {
            binding.prayerCountdown.text = "- 00:00:00"
        }
    }

    private fun getNextDayFajrTime(fajrTime: String): Long {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val fajrDate = sdf.parse(fajrTime) ?: return 0L
            val currentCalendar = Calendar.getInstance()
            val nextDayCalendar = Calendar.getInstance()
            nextDayCalendar.add(Calendar.DAY_OF_MONTH, 1)

            val fajrCalendar = Calendar.getInstance()
            fajrCalendar.time = fajrDate

            nextDayCalendar.set(Calendar.HOUR_OF_DAY, fajrCalendar.get(Calendar.HOUR_OF_DAY))
            nextDayCalendar.set(Calendar.MINUTE, fajrCalendar.get(Calendar.MINUTE))
            nextDayCalendar.set(Calendar.SECOND, 0)
            nextDayCalendar.set(Calendar.MILLISECOND, 0)

            return nextDayCalendar.timeInMillis
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun setDataViewWithCountdown(
        prayerName: String,
        prayerTime: String,
        timeRemaining: Long,
    ) {
        binding.salahName.text = prayerName
        binding.prayerTime.text = formatSalahTimeForLocale(prayerTime)

        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                binding.prayerCountdown.text =
                    "- ${CommonUtils.updateCountDownText(millisUntilFinished)}"
            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                binding.prayerCountdown.text = "- 00:00:00"
            }
        }
        countDownTimer?.start()
    }

    private fun startElapsedTimeCounter() {
        elapsedTimeRunnable = object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                timeElapsed =
                    CommonUtils.getTimeLong(CommonUtils.getCurrentTime(), true) - timeStarted
                binding.prayerCountdown.text = "+ ${CommonUtils.updateCountDownText(timeElapsed)}"
                handler.postDelayed(this, 1000)
            }
        }
        elapsedTimeRunnable?.let { handler.post(it) }
    }

    private fun stopAllTimers() {
        countDownTimer?.cancel()
        countDownTimer = null

        elapsedTimeRunnable?.let {
            handler.removeCallbacks(it)
            elapsedTimeRunnable = null
        }
    }

    private fun showLoadingState() {
        binding.fajrTime.text = getString(R.string.loading)
        binding.sunriseTime.text = getString(R.string.loading)
        binding.dhuhrTime.text = getString(R.string.loading)
        binding.asrTime.text = getString(R.string.loading)
        binding.maghribTime.text = getString(R.string.loading)
        binding.ishaTime.text = getString(R.string.loading)
        binding.salahName.text = getString(R.string.loading)
        binding.prayerTime.text = getString(R.string.loading)
        binding.prayerCountdown.text = getString(R.string.loading)
    }

    private fun setupPrayerNotificationCheckboxes(editor: SharedPreferences.Editor) {

        binding.cbFajr.isChecked = sharedPreference.getBoolean(Constants.AZAN.FAJR, false)
        binding.cbDhuhr.isChecked = sharedPreference.getBoolean(Constants.AZAN.DUHR, false)
        binding.cbAsr.isChecked = sharedPreference.getBoolean(Constants.AZAN.ASR, false)
        binding.cbMaghrib.isChecked = sharedPreference.getBoolean(Constants.AZAN.MAGREB, false)
        binding.cbIsha.isChecked = sharedPreference.getBoolean(Constants.AZAN.ISHA, false)


        binding.cbFajr.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.FAJR, isChecked).apply()
            if (isChecked) {
                notifyAzan(
                    requireContext(),
                    prayerTimeAlarm.Fajr,
                    10,
                    getString(R.string.fajr),
                    getString(R.string.salahNow),
                    10
                )
            } else {
                cancelAlarm(requireContext(), 10)
            }
        }

        binding.cbDhuhr.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.DUHR, isChecked).apply()
            if (isChecked) {
                notifyAzan(
                    requireContext(),
                    prayerTimeAlarm.Dhuhr,
                    11,
                    getString(R.string.duhr),
                    getString(R.string.salahNow),
                    11
                )
            } else {
                cancelAlarm(requireContext(), 11)
            }
        }

        binding.cbAsr.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.ASR, isChecked).apply()
            if (isChecked) {
                notifyAzan(
                    requireContext(),
                    prayerTimeAlarm.Asr,
                    12,
                    getString(R.string.asr),
                    getString(R.string.salahNow),
                    12
                )
            } else {
                cancelAlarm(requireContext(), 12)
            }
        }

        binding.cbMaghrib.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.MAGREB, isChecked).apply()
            if (isChecked) {
                notifyAzan(
                    requireContext(),
                    prayerTimeAlarm.Maghrib,
                    13,
                    getString(R.string.maghreb),
                    getString(R.string.salahNow),
                    13
                )
            } else {
                cancelAlarm(requireContext(), 13)
            }
        }

        binding.cbIsha.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.ISHA, isChecked).apply()
            if (isChecked) {
                notifyAzan(
                    requireContext(),
                    prayerTimeAlarm.Isha,
                    14,
                    getString(R.string.isha),
                    getString(R.string.salahNow),
                    14
                )
            } else {
                cancelAlarm(requireContext(), 14)
            }
        }
    }

    private fun setupClickListeners(drawerLayout: DrawerLayout, navView: NavigationView) {
        binding.update.setOnClickListener {
            if (!MethodHelper.isOnline(requireContext())) {
                showNoInternetDialog()
                return@setOnClickListener
            }

            stopLocationUpdates()
            requestLocationUpdate()
            Toast.makeText(requireContext(), "جارى تحديث أوقات الصلاة", Toast.LENGTH_SHORT).show()
        }

        binding.qibla.setOnClickListener {
            val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
            val savedLongitude = sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()

            if (savedLatitude != 0.0 && savedLongitude != 0.0) {
                if (MethodHelper.isOnline(requireContext())) {
                    val bundle = Bundle()
                    bundle.putString("altitude", savedLatitude.toString())
                    bundle.putString("longitude", savedLongitude.toString())
                    findNavController().navigate(
                        R.id.action_navigation_home_to_qiblaFragment2,
                        bundle
                    )
                } else {
                    MethodHelper.toastMessage(getString(R.string.checkConnection))
                }
            } else {
                MethodHelper.toastMessage(getString(R.string.requiredLocation))
            }
        }

        binding.openDrawable.setOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.rateApp -> rateApp()
                R.id.projectLink -> projectLink()
                R.id.shareApp -> {
                    MethodHelper.shareApp(
                        requireContext(), getString(R.string.app_name),
                        "https://play.google.com/store/apps/details?id=com.megahed.eqtarebmenalla"
                    )
                }

                R.id.setting -> {
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.close()
                }

                else -> {
                    drawerLayout.closeDrawers()
                }
            }
            true
        }
    }

    private fun setPrayerTimesUpdateWorker(location: Location) {
        activity?.let {
            PrayerTimesScheduler.schedulePrayerTimesWork(it, location.latitude, location.longitude)
        }
    }

    private fun updateAzan() {

        val azanSettings = mapOf(
            Constants.AZAN.FAJR to Triple(prayerTimeAlarm.Fajr, 10, getString(R.string.fajr)),
            Constants.AZAN.DUHR to Triple(prayerTimeAlarm.Dhuhr, 11, getString(R.string.duhr)),
            Constants.AZAN.ASR to Triple(prayerTimeAlarm.Asr, 12, getString(R.string.asr)),
            Constants.AZAN.MAGREB to Triple(
                prayerTimeAlarm.Maghrib,
                13,
                getString(R.string.maghreb)
            ),
            Constants.AZAN.ISHA to Triple(prayerTimeAlarm.Isha, 14, getString(R.string.isha))
        )

        azanSettings.forEach { (key, value) ->
            if (sharedPreference.getBoolean(key, false)) {
                notifyAzan(
                    requireContext(),
                    value.first,
                    value.second,
                    value.third,
                    getString(R.string.salahNow),
                    value.second
                )
            }
        }
    }

    private fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data =
            "https://play.google.com/store/apps/details?id=com.megahed.eqtarebmenalla".toUri()
        startActivity(intent)
    }

    private fun projectLink() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = "https://github.com/MohamedAshraf9/ToAllah".toUri()
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun checkLocationPermission() {
        val perms = arrayOf(
            // Only request coarse location for approximate location
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (MethodHelper.hasPermissions(requireContext(), perms)) {
            startGetLocation()
        } else {
            requestPermissionLauncher.launch(perms)
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            startGetLocation()
        } else {
            binding.currentLocation.text = "Location permission denied"
            isUpdatingLocation = false
            isLocationUpdateRequested = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGetLocation() {
        Log.d("HomeFragment", "Starting location updates...")
        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )

        mFusedLocationClient.lastLocation
            .addOnSuccessListener(requireActivity()) { location ->
                location?.let {
                    Log.d(
                        "HomeFragment",
                        "Got last known location: ${it.latitude}, ${it.longitude}"
                    )
                    val locationTime = System.currentTimeMillis() - it.time
                    if (locationTime < 300000 && isUpdatingLocation) {
                        handleLocationUpdate(it)
                        stopLocationUpdates()
                    }
                }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w("HomeFragment", "getLastLocation:exception " + e.message)
            }
    }

    @SuppressLint("SimpleDateFormat")
    fun notifyAzan(
        context: Context,
        myTime: String,
        alarmId: Int,
        title: String,
        desc: String,
        notificationId: Int,
    ) {
        val df = SimpleDateFormat("HH:mm")
        val d: Date = df.parse(myTime)!!
        val cal = Calendar.getInstance()
        cal.time = d
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, 0)

        val now = Calendar.getInstance()
        if (now.time >= calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)

        val calendar1 = calendar.clone() as Calendar
        val intent = Intent(context, NotifyMessing::class.java)
        intent.putExtra("AlarmTitle", title)
        intent.putExtra(
            "AlarmNote",
            desc + "\n" + com.megahed.eqtarebmenalla.common.Constants.maw3idha[(0..10).random()]
        )
        intent.putExtra(
            "AlarmColor",
            ContextCompat.getColor(App.getInstance(), R.color.colorPrimary)
        )
        intent.putExtra("interval", "daily")
        intent.putExtra("notificationId", notificationId)
        intent.action = "com.megahed.eqtarebmenalla.TIMEALARM"

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            (App.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar1.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar1.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context, alarmId: Int) {
        val intent = Intent(context, NotifyMessing::class.java)
        intent.action = "com.megahed.eqtarebmenalla.TIMEALARM"
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun statusLocationCheck() {
        val manager =
            requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            turnGPSOn()
        } else {
            checkLocationPermission()
        }
    }

    private fun turnGPSOn() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            checkLocationPermission()
        }

        task.addOnSuccessListener {
            checkLocationPermission()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(requireActivity(), LOCATION_REQUEST_CODE)
                } catch (_: IntentSender.SendIntentException) {
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCountryFromLocation(
        location: Location,
        onLocationNameReceived: (String) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val locationName = GeocodingHelper.getAddressFromLocation(
                requireContext(),
                location.latitude,
                location.longitude
            )
            onLocationNameReceived(locationName ?: "Unknown Location")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAllTimers()
        stopLocationUpdates()
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("لا يوجد إتصال بالإنترنت")
            .setMessage("يلزم الاتصال بالإنترنت لتحديث أوقات الصلاة. يُرجى الاتصال بالإنترنت والمحاولة مرة أخرى.")
            .setPositiveButton("حسناً") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("الإعدادات") { dialog, _ ->
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Could not open settings: ${e.message}")
                }
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun showNoInternetWarning() {
        AlertDialog.Builder(requireContext())
            .setTitle("تحديث الموقع")
            .setMessage("تم تحديث موقعك، ولكن يلزم اتصال بالإنترنت لعرض أحدث مواقيت الصلاة. يُرجى الاتصال بالإنترنت للحصول على مواقيت الصلاة المُحدَّثة.")
            .setPositiveButton("حسسناً") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("حاول مجدداً") { dialog, _ ->
                if (MethodHelper.isOnline(requireContext())) {
                    val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
                    val savedLongitude = sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()
                    if (savedLatitude != 0.0 && savedLongitude != 0.0) {
                        mainViewModel.getAzanData(savedLatitude, savedLongitude)
                        Toast.makeText(
                            requireContext(),
                            "جاري تحديث أوقات الصلاة ...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showNoInternetDialog()
                }
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}