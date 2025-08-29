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
    private lateinit var binding: FragmentHomeBinding

    private var countDownTimer: CountDownTimer? = null
    private var timeStarted: Long = 0
    private var timeElapsed: Long = 0
    private var isUpdatingLocation = false
    private var isLocationUpdateRequested = false

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedTimeRunnable: Runnable? = null

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

        setupPrayerNotificationCheckboxes(editor)

        setupClickListeners(drawerLayout, navView)

        return root
    }

    private fun setupLocationRequest() {
        // Updated to use PRIORITY_BALANCED_POWER_ACCURACY for approximate location
        locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000) // 30 seconds interval
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(30000)
            .setMaxUpdateDelayMillis(60000)
            .build()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("HomeFragment", "Location received: ${location.latitude}, ${location.longitude}")
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
            val savedLocationName = sharedPreference.getString(PREF_LOCATION_NAME, "Unknown Location")

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
        Log.d("HomeFragment", "Location updated and saved: ${location.latitude}, ${location.longitude}")
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

    private fun updatePrayerTimesUI(prayerTime: PrayerTime) {
        binding.fajrTime.text = CommonUtils.convertSalahTime(prayerTime.Fajr)
        binding.sunriseTime.text = CommonUtils.convertSalahTime(prayerTime.Sunrise)
        binding.dhuhrTime.text = CommonUtils.convertSalahTime(prayerTime.Dhuhr)
        binding.asrTime.text = CommonUtils.convertSalahTime(prayerTime.Asr)
        binding.maghribTime.text = CommonUtils.convertSalahTime(prayerTime.Maghrib)
        binding.ishaTime.text = CommonUtils.convertSalahTime(prayerTime.Isha)
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
                // After Isha, show countdown to next day's Fajr
                showCountdownToNextDayFajr(prayerTime)
            }
        }
    }

    private fun showCountdownToNextDayFajr(prayerTime: PrayerTime) {
        binding.salahName.text = getString(R.string.fajr)

        // Calculate next day's Fajr time
        val nextDayFajrTimeMillis = getNextDayFajrTime(prayerTime.Fajr)
        val currentTimeMillis = System.currentTimeMillis()
        val timeRemaining = nextDayFajrTimeMillis - currentTimeMillis

        binding.prayerTime.text = CommonUtils.convertSalahTime(prayerTime.Fajr)

        Log.d("HomeFragment", "Next Fajr millis: $nextDayFajrTimeMillis")
        Log.d("HomeFragment", "Current millis: $currentTimeMillis")
        Log.d("HomeFragment", "Time remaining: $timeRemaining")

        if (timeRemaining > 0) {
            countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    binding.prayerCountdown.text = "- ${CommonUtils.updateCountDownText(millisUntilFinished)}"
                }

                @SuppressLint("SetTextI18n")
                override fun onFinish() {
                    binding.prayerCountdown.text = "- 00:00:00"
                    // Refresh prayer times for the new day
                    if (MethodHelper.isOnline(requireContext())) {
                        val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
                        val savedLongitude = sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()
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

            // Get current date and time
            val currentCalendar = Calendar.getInstance()

            // Create calendar for tomorrow's Fajr
            val nextDayCalendar = Calendar.getInstance()
            nextDayCalendar.add(Calendar.DAY_OF_MONTH, 1) // Move to next day

            // Extract hour and minute from Fajr time string
            val fajrCalendar = Calendar.getInstance()
            fajrCalendar.time = fajrDate

            // Set tomorrow's date with Fajr time
            nextDayCalendar.set(Calendar.HOUR_OF_DAY, fajrCalendar.get(Calendar.HOUR_OF_DAY))
            nextDayCalendar.set(Calendar.MINUTE, fajrCalendar.get(Calendar.MINUTE))
            nextDayCalendar.set(Calendar.SECOND, 0)
            nextDayCalendar.set(Calendar.MILLISECOND, 0)

            Log.d("HomeFragment", "Current time: ${currentCalendar.time}")
            Log.d("HomeFragment", "Next Fajr time: ${nextDayCalendar.time}")
            Log.d("HomeFragment", "Time difference in millis: ${nextDayCalendar.timeInMillis - currentCalendar.timeInMillis}")

            return nextDayCalendar.timeInMillis
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error calculating next day Fajr time: ${e.message}")
            return 0L
        }
    }

    private fun setDataViewWithCountdown(prayerName: String, prayerTime: String, timeRemaining: Long) {
        binding.salahName.text = prayerName
        binding.prayerTime.text = prayerTime

        countDownTimer = object : CountDownTimer(timeRemaining, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                binding.prayerCountdown.text = "- ${CommonUtils.updateCountDownText(millisUntilFinished)}"
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
                timeElapsed = CommonUtils.getTimeLong(CommonUtils.getCurrentTime(), true) - timeStarted
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
                notifyAzan(requireContext(), prayerTimeAlarm.Fajr, 10, getString(R.string.fajr), getString(R.string.salahNow), 10)
            } else {
                cancelAlarm(requireContext(), 10)
            }
        }

        binding.cbDhuhr.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.DUHR, isChecked).apply()
            if (isChecked) {
                notifyAzan(requireContext(), prayerTimeAlarm.Dhuhr, 11, getString(R.string.duhr), getString(R.string.salahNow), 11)
            } else {
                cancelAlarm(requireContext(), 11)
            }
        }

        binding.cbAsr.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.ASR, isChecked).apply()
            if (isChecked) {
                notifyAzan(requireContext(), prayerTimeAlarm.Asr, 12, getString(R.string.asr), getString(R.string.salahNow), 12)
            } else {
                cancelAlarm(requireContext(), 12)
            }
        }

        binding.cbMaghrib.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.MAGREB, isChecked).apply()
            if (isChecked) {
                notifyAzan(requireContext(), prayerTimeAlarm.Maghrib, 13, getString(R.string.maghreb), getString(R.string.salahNow), 13)
            } else {
                cancelAlarm(requireContext(), 13)
            }
        }

        binding.cbIsha.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean(Constants.AZAN.ISHA, isChecked).apply()
            if (isChecked) {
                notifyAzan(requireContext(), prayerTimeAlarm.Isha, 14, getString(R.string.isha), getString(R.string.salahNow), 14)
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
                    findNavController().navigate(R.id.action_navigation_home_to_qiblaFragment2, bundle)
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
            Constants.AZAN.MAGREB to Triple(prayerTimeAlarm.Maghrib, 13, getString(R.string.maghreb)),
            Constants.AZAN.ISHA to Triple(prayerTimeAlarm.Isha, 14, getString(R.string.isha))
        )

        azanSettings.forEach { (key, value) ->
            if (sharedPreference.getBoolean(key, false)) {
                notifyAzan(requireContext(), value.first, value.second, value.third, getString(R.string.salahNow), value.second)
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
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())

        mFusedLocationClient.lastLocation
            .addOnSuccessListener(requireActivity()) { location ->
                location?.let {
                    Log.d("HomeFragment", "Got last known location: ${it.latitude}, ${it.longitude}")
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
    fun notifyAzan(context: Context, myTime: String, alarmId: Int, title: String, desc: String, notificationId: Int) {
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
        intent.putExtra("AlarmNote", desc + "\n" + com.megahed.eqtarebmenalla.common.Constants.maw3idha[(0..10).random()])
        intent.putExtra("AlarmColor", ContextCompat.getColor(App.getInstance(), R.color.colorPrimary))
        intent.putExtra("interval", "daily")
        intent.putExtra("notificationId", notificationId)
        intent.action = "com.megahed.eqtarebmenalla.TIMEALARM"

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = (App.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar1.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar1.timeInMillis, pendingIntent)
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
        val manager = requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
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
    private fun getCountryFromLocation(location: Location, onLocationNameReceived: (String) -> Unit) {
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
            .setTitle("Location Updated")
            .setMessage("Your location has been updated, but internet connection is required to fetch the latest prayer times. Please connect to the internet to get updated prayer times.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Retry") { dialog, _ ->
                if (MethodHelper.isOnline(requireContext())) {
                    val savedLatitude = sharedPreference.getFloat(PREF_LATITUDE, 0f).toDouble()
                    val savedLongitude = sharedPreference.getFloat(PREF_LONGITUDE, 0f).toDouble()
                    if (savedLatitude != 0.0 && savedLongitude != 0.0) {
                        mainViewModel.getAzanData(savedLatitude, savedLongitude)
                        Toast.makeText(requireContext(), "Updating prayer times...", Toast.LENGTH_SHORT).show()
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