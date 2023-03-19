package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.AlarmManager.AlarmClockInfo
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


@AndroidEntryPoint
class HomeFragment : Fragment(), LocationListener {

    var prayerTimeAlarm:PrayerTime= PrayerTime()

    lateinit var  sharedPreference : SharedPreferences


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location?=null
    private lateinit var locationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel
    lateinit var builder : Notification.Builder
    //lateinit var loadingAlert: LoadingAlert

    ///var isDataAdded:Boolean=false


    private val mainViewModel : IslamicViewModel by activityViewModels()


    private lateinit var binding: FragmentHomeBinding
    private var timeStarted : Long = 0
    private var timeElapsed : Long = 0
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val prayerTimeViewModel =
            ViewModelProvider(this).get(PrayerTimeViewModel::class.java)

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // create loading alert

        //loadingAlert = LoadingAlert(requireActivity())
        //loadingAlert.startLoadingAlert()


        sharedPreference =  requireActivity().getSharedPreferences("adhen",Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()



        if (!sharedPreference.getBoolean("firstTime", false) ){
            val alert = Alert(requireActivity()).startLoadingAlert()
            editor.putBoolean("firstTime", true)
            editor.apply()

        }
       binding.dayDetails.text= DateFormat.getDateInstance(DateFormat.FULL).format(Date())

        lifecycleScope.launchWhenStarted {
            mainViewModel.state.collect{ islamicListState ->
                islamicListState.let { islamicInfo ->
                    islamicInfo.islamicInfo.data?.let {
                        val prayerTime= PrayerTime(1,it.date.gregorian.date,it.timings.Asr,it.timings.Dhuhr,
                            it.timings.Fajr,it.timings.Isha,it.timings.Maghrib,it.timings.Sunrise)
                        //isDataAdded=true
                        prayerTimeViewModel.insertPrayerTime(prayerTime)


                    }
                }
            }


        }

        lifecycleScope.launchWhenStarted {
            prayerTimeViewModel.getPrayerTimeById().collect {
                it?.let {
                    //if (isDataAdded) {

                    prayerTimeAlarm=it
                    binding.fajrTime.text = CommonUtils.convertSalahTime(it.Fajr)
                    binding.sunriseTime.text = CommonUtils.convertSalahTime(it.Sunrise)
                    binding.dhuhrTime.text = CommonUtils.convertSalahTime(it.Dhuhr)
                    binding.asrTime.text = CommonUtils.convertSalahTime(it.Asr)
                    binding.maghribTime.text = CommonUtils.convertSalahTime(it.Maghrib)
                    binding.ishaTime.text = CommonUtils.convertSalahTime(it.Isha)
                    if (MethodHelper.isOnline(requireContext()))
                        updateAzan()
                    //loadingAlert.dismissDialog()


                    val currentTime = CommonUtils.getCurrentTime()
                    if (CommonUtils.getTimeLong(it.Fajr, false) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {

                        setDataView(
                            getString(R.string.fajr), CommonUtils.convertSalahTime(it.Fajr),
                            CommonUtils.getTimeLong(it.Fajr, false) - CommonUtils.getTimeLong(
                                currentTime,
                                true
                            ), true
                        )

                    } else if (CommonUtils.getTimeLong(
                            it.Sunrise,
                            false
                        ) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {
                        setDataView(
                            getString(R.string.sunrise), CommonUtils.convertSalahTime(it.Sunrise),
                            CommonUtils.getTimeLong(it.Sunrise, false) - CommonUtils.getTimeLong(
                                currentTime,
                                true
                            ), true
                        )

                    } else if (CommonUtils.getTimeLong(it.Dhuhr, false) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {

                        setDataView(
                            getString(R.string.duhr), CommonUtils.convertSalahTime(it.Dhuhr),
                            CommonUtils.getTimeLong(it.Dhuhr, false) - CommonUtils.getTimeLong(
                                currentTime,
                                true
                            ), true
                        )

                    } else if (CommonUtils.getTimeLong(it.Asr, false) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {

                        setDataView(
                            getString(R.string.asr), CommonUtils.convertSalahTime(it.Asr),
                            CommonUtils.getTimeLong(
                                it.Asr,
                                false
                            ) - CommonUtils.getTimeLong(currentTime, true), true
                        )

                    } else if (CommonUtils.getTimeLong(
                            it.Maghrib,
                            false
                        ) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {
                        setDataView(
                            getString(R.string.maghreb), CommonUtils.convertSalahTime(it.Maghrib),
                            CommonUtils.getTimeLong(it.Maghrib, false) - CommonUtils.getTimeLong(
                                currentTime,
                                true
                            ), true
                        )


                    } else if (CommonUtils.getTimeLong(it.Isha, false) >= CommonUtils.getTimeLong(
                            currentTime,
                            true
                        )
                    ) {
                        setDataView(
                            getString(R.string.isha), CommonUtils.convertSalahTime(it.Isha),
                            CommonUtils.getTimeLong(it.Isha, false) - CommonUtils.getTimeLong(
                                currentTime,
                                true
                            ), true
                        )
                    } else {
                        setDataView(
                            getString(R.string.isha), CommonUtils.convertSalahTime(it.Isha),
                            0, false
                        )

                        timeStarted = CommonUtils.getTimeLong(it.Isha, false)
                        startCoroutineTimer()

                    }

                //}
            }
                 ?: run {
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
            }
        }



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationRequest= LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY,100)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(100)
            .setMaxUpdateDelayMillis(800)
            .build()


        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Update UI with location data
                    //Log.d("gfdgdfg lastLocation.isMock", ""+ location.isMock)
                    lastLocation = location
                    lastLocation?.let {
                        getCountryFromLocation(it)


                        mainViewModel.getAzanData(it.latitude,it.longitude)

                    }
                    Log.d(
                        "myLocccc",
                        " callback location " + lastLocation?.longitude + "  " + lastLocation?.latitude
                    )
                }
            }

        }



        if (sharedPreference.getBoolean(Constants.AZAN.FAJR,false) ){
            binding.cbFajr.isChecked = true
        }
        if (sharedPreference.getBoolean(Constants.AZAN.DUHR,false) ){
            binding.cbDhuhr.isChecked = true
        }
        if (sharedPreference.getBoolean(Constants.AZAN.ASR,false) ){
            binding.cbAsr.isChecked = true
        }
        if (sharedPreference.getBoolean(Constants.AZAN.MAGREB,false) ){
            binding.cbMaghrib.isChecked = true
        }

        if (sharedPreference.getBoolean(Constants.AZAN.ISHA,false) ){
            binding.cbIsha.isChecked = true
        }



        binding.cbFajr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbFajr.isChecked){

                editor.putBoolean(Constants.AZAN.FAJR,true)
                editor.apply()


               notifyAzan(requireContext(),prayerTimeAlarm.Fajr,10,getString(R.string.fajr),getString(R.string.salahNow),10)



            }
            else{
                editor.putBoolean(Constants.AZAN.FAJR,false)
                editor.apply()

                cancelAlarm(requireContext(),10)


            }
        }

        binding.cbDhuhr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbDhuhr.isChecked){

                editor.putBoolean(Constants.AZAN.DUHR,true)
                editor.apply()

                notifyAzan(requireContext(),prayerTimeAlarm.Dhuhr,11,getString(R.string.duhr),getString(R.string.salahNow),11)




            }
            else {
                editor.putBoolean(Constants.AZAN.DUHR, false)
                editor.apply()

                cancelAlarm(requireContext(),11)
            }
        }


        binding.cbAsr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbAsr.isChecked){

                editor.putBoolean(Constants.AZAN.ASR,true)
                editor.apply()



                notifyAzan(requireContext(),prayerTimeAlarm.Asr,12,getString(R.string.asr),getString(R.string.salahNow),12)



            }
            else {
                editor.putBoolean(Constants.AZAN.ASR, false)
                editor.apply()

                cancelAlarm(requireContext(),12)
            }
        }


        binding.cbMaghrib.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbMaghrib.isChecked){

                editor.putBoolean(Constants.AZAN.MAGREB,true)
                editor.apply()

                notifyAzan(requireContext(),prayerTimeAlarm.Maghrib,13,getString(R.string.maghreb),getString(R.string.salahNow),13)




            }
            else {
                editor.putBoolean(Constants.AZAN.MAGREB, false)
                editor.apply()

                cancelAlarm(requireContext(),13)
            }
        }


        binding.cbIsha.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbIsha.isChecked){

                editor.putBoolean(Constants.AZAN.ISHA,true)
                editor.apply()

                notifyAzan(requireContext(),prayerTimeAlarm.Isha,14,getString(R.string.isha),getString(R.string.salahNow),14)



            }
            else {
                editor.putBoolean(Constants.AZAN.ISHA, false)
                editor.apply()

                cancelAlarm(requireContext(),14)
            }

        }



        binding.update.setOnClickListener {
            updateAzan()

            Toast.makeText(requireContext(), "جارى تحديث أوقات الصلاة", Toast.LENGTH_LONG).show()
        }


        binding.qibla.setOnClickListener {

//            val intent = Intent(requireContext(), QiblaActivity::class.java)
//            requireContext().startActivity(intent)

           lastLocation?.let {
               if (MethodHelper.isOnline(requireContext())){
                   val bundle = Bundle()
                   bundle.putString("altitude", lastLocation?.altitude.toString())
                   bundle.putString("longitude", lastLocation?.longitude.toString())

                   findNavController().navigate(R.id.action_navigation_home_to_qiblaFragment2, bundle)
               }
               else{
                   MethodHelper.toastMessage(getString(R.string.checkConnection))
               }
           }?:run {
               MethodHelper.toastMessage(getString(R.string.requiredLocation))
           }




        }

        binding.openDrawable.setOnClickListener {
            drawerLayout.open()
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.rateApp -> {
                    rateApp()
                }
                R.id.projectLink -> {
                   projectLink()
                }
                R.id.shareApp -> {
                    MethodHelper.shareApp(
                        requireContext(), getString(R.string.app_name),
                        "https://play.google.com/store/apps/details?id=com.megahed.eqtarebmenalla"
                    )

                }
                else -> {
                    drawerLayout.closeDrawers()
                }

            }

            true
        }

        return root
    }

    private fun updateAzan() {
        statusLocationCheck()

        if (sharedPreference.getBoolean(Constants.AZAN.FAJR,false) ){

            notifyAzan(requireContext(),prayerTimeAlarm.Fajr,10,getString(R.string.fajr),getString(R.string.salahNow),10)

        }

        if (sharedPreference.getBoolean(Constants.AZAN.DUHR,false) ){

            notifyAzan(requireContext(),prayerTimeAlarm.Dhuhr,11,getString(R.string.duhr),getString(R.string.salahNow),11)


        }

        if (sharedPreference.getBoolean(Constants.AZAN.ASR,false) ){
            notifyAzan(requireContext(),prayerTimeAlarm.Asr,12,getString(R.string.asr),getString(R.string.salahNow),12)


        }


        if (sharedPreference.getBoolean(Constants.AZAN.MAGREB,false) ){

            notifyAzan(requireContext(),prayerTimeAlarm.Maghrib,13,getString(R.string.maghreb),getString(R.string.salahNow),13)


        }

        if (sharedPreference.getBoolean(Constants.AZAN.ISHA,false) ){
            notifyAzan(requireContext(),prayerTimeAlarm.Isha,14,getString(R.string.isha),getString(R.string.salahNow),14)


        }
    }

    private fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data =
            Uri.parse("https://play.google.com/store/apps/details?id=com.megahed.eqtarebmenalla")
        startActivity(intent)
    }
    private fun projectLink() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data =
            Uri.parse("https://github.com/MohamedAshraf9/ToAllah")
        startActivity(intent)
    }

    fun getSpannable(text: String): Spannable? {
        val spannable: Spannable = SpannableString(text)
        val REGEX = "لل"
        val p = Pattern.compile(REGEX)
        val m = p.matcher(text)
        var start: Int
        var end: Int

        //region allah match
        while (m.find()) {
            start = m.start()
            while (text[start] != ' ' && start != 0) {
                start--
            }
            end = m.end()
            while (text[end] != ' ') {
                end++
            }
            spannable.setSpan(
                ForegroundColorSpan(Color.RED),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        //endregion
        return spannable
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }



    private fun setDataView(prayerName: String, prayerTime: String, time:Long, countDown:Boolean) {
        binding.salahName.text= prayerName
        binding.prayerTime.text= prayerTime


        if (countDown) {
            val timer = object : CountDownTimer(
                time, 1000
            ) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    binding.prayerCountdown.text = "- ${CommonUtils.updateCountDownText(millisUntilFinished)}"
                }

                override fun onFinish() {

                }


            }
            timer.start()
        }
    }



    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = object : Runnable {
        override fun run() {
            timeElapsed = CommonUtils.getTimeLong(CommonUtils.getCurrentTime(),true) - timeStarted
            binding.prayerCountdown.text="+ ${CommonUtils.updateCountDownText(timeElapsed)}"
            // Repeat every 1 second
            handler.postDelayed(this, 1000)
        }
    }

    private fun startCoroutineTimer() {
        handler.post(runnable)
    }





    override fun onStart() {
        super.onStart()
        statusLocationCheck()
        //checkLocationPermission()
    }
    override fun onPause() {
        super.onPause()
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }



    @SuppressLint("MissingPermission")
    private fun checkLocationPermission() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (MethodHelper.hasPermissions(requireContext(), perms)) {

            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback,  Looper.getMainLooper())

            startGetLocation()
        } else {
            //statusLocationCheck()
            requestPermissionLauncher.launch(perms)

        }
    }


    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all {
            it.value
        }
        if (granted) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null)
            startGetLocation()
            // PERMISSION GRANTED
        } else {
            // PERMISSION NOT GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGetLocation() {
        mFusedLocationClient.lastLocation
            .addOnSuccessListener(
                requireActivity()
            ) { location ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    lastLocation = location
                    Log.d(
                        "myLocccc",
                        " last  " + lastLocation?.longitude + "  " + lastLocation?.latitude
                    )

                }
            }
            .addOnFailureListener(
                requireActivity()
            ) { e ->
                Log.w("myLocccc", "getLastLocation:exception " + e.message)
            }

    }


    override fun onLocationChanged(location: Location) {
        lastLocation = location
        lastLocation?.let {
            mainViewModel.getAzanData(it.latitude,it.longitude)
        }

    }


    @SuppressLint("SimpleDateFormat")
    fun notifyAzan(context: Context, myTime: String, alarmId: Int, title:String, desc:String, notificationId:Int) {
        val df = SimpleDateFormat("HH:mm")
        val d: Date = df.parse(myTime)!!
        val cal = Calendar.getInstance()
        cal.time = d
        val calendar=Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY,cal.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE,cal.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND,0)


        val now = Calendar.getInstance()
        if (now.time>=calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)

        Log.d("sdsdsd",calendar.time.time.toString())
        val calendar1 = calendar.clone() as Calendar
        val intent = Intent(context, NotifyMessing::class.java)
        intent.putExtra("AlarmTitle", title)
        intent.putExtra("AlarmNote",desc+"\n"+ com.megahed.eqtarebmenalla.common.Constants.maw3idha[(0..10).random()])
        intent.putExtra("AlarmColor", ContextCompat.getColor(App.getInstance(), R.color.colorPrimary))
        intent.putExtra("interval", "daily")
        intent.putExtra("notificationId", notificationId)
        intent.action = "com.megahed.eqtarebmenalla.TIMEALARM"
        @SuppressLint("UnspecifiedImmutableFlag")
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
            alarmManager.setAlarmClock(
                AlarmClockInfo(calendar1.timeInMillis, pendingIntent),
                pendingIntent
            )

        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    fun cancelAlarm(context: Context, alarmId: Int) {
        val intent = Intent(context, NotifyMessing::class.java)
        intent.action = "com.megahed.eqtarebmenalla.TIMEALARM"
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }


    private fun statusLocationCheck() {
        val manager = requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            turnGPSOn()
            //buildAlertMessageNoGps()

        }else{
            checkLocationPermission()
        }
    }


    private fun turnGPSOn() {
        //val builder = LocationSettingsRequest.Builder()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())



        task.addOnCompleteListener {
            Log.d("fdfdf", "cccc dddddddddddddd ${it.isSuccessful}")
            checkLocationPermission()

        }
        task.addOnSuccessListener {
            Log.d("fdfdf", "sssssss dddddddddddddd ${it.locationSettingsStates}")
            checkLocationPermission()
        }


        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(requireActivity(),
                        1)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.openLocation))
            .setCancelable(false)
            .setPositiveButton(
                getString(R.string.openSettings)
            ) { dialog, id ->

                //startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(
                getString(R.string.cancel)
            ) { dialog, id ->
                dialog.cancel()

            }
        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("SetTextI18n")
    private fun getCountryFromLocation(location: Location) {
        //val local = Locale("en")
        val geocoder = Geocoder(requireContext())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude,location.longitude,1,object : Geocoder.GeocodeListener{
                override fun onGeocode(addresses: MutableList<Address>) {
                    // code
                    if (addresses.size > 0) {
                        val country = addresses[0].adminArea
                        val city = addresses[0].subAdminArea
                        binding.currentLocation.text = "$city,$country"
                        //Log.d("eyrtewew",addresses.toString())

                        //Toast.makeText(requireContext(),"$city,$country ", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)

                }

            })
        }
        else{
            try {
                Geocoder.isPresent()
                val addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
                )

                if (addresses != null && addresses.size > 0) {
                    val country = addresses[0].adminArea
                    val city = addresses[0].subAdminArea
                    binding.currentLocation.text = "$city,$country"

                    //Log.d("eyrtewew",addresses.toString())
                    //Toast.makeText(requireContext(),"$city,$country ", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                e.message?.let { Log.d("myLocccc Exception  ", it) }
            }
        }

        //Log.d("eyrtewew",binding.currentLocation.text.toString())

        //var country: String=""

        //return country?.lowercase()
    }



        var lang = ""
        var lati = ""


}