package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.common.LoadingAlert
import com.megahed.eqtarebmenalla.databinding.FragmentHomeBinding
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.feature_data.data.remote.adhen.MyBroadcastReceiver
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import dagger.hilt.android.AndroidEntryPoint
import de.coldtea.smplr.smplralarm.*
import java.io.IOException
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern


@AndroidEntryPoint
class HomeFragment : Fragment(), LocationListener {


    lateinit var  sharedPreference : SharedPreferences


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location?=null
    private lateinit var locationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel
    lateinit var builder : Notification.Builder
    lateinit var loadingAlert: LoadingAlert



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

        loadingAlert = LoadingAlert(requireActivity())
        loadingAlert.startLoadingAlert()


        sharedPreference =  requireActivity().getSharedPreferences("adhen",Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()



        if (sharedPreference.getString("firstTime", "") != "false"){
            val alert = Alert(requireActivity()).startLoadingAlert()
            editor.putString("firstTime", "false");
            editor.commit()

        }
       binding.dayDetails.text= DateFormat.getDateInstance(DateFormat.FULL).format(Date())

        lifecycleScope.launchWhenStarted {
            mainViewModel.state.collect{ islamicListState ->
                islamicListState.let { islamicInfo ->
                    islamicInfo.islamicInfo.data?.let {
                        val prayerTime= PrayerTime(1,it.date.gregorian.date,it.timings.Asr,it.timings.Dhuhr,
                            it.timings.Fajr,it.timings.Isha,it.timings.Maghrib,it.timings.Sunrise)
                        prayerTimeViewModel.insertPrayerTime(prayerTime)

                    }
                }
            }


        }

        lifecycleScope.launchWhenStarted {
            prayerTimeViewModel.getPrayerTimeById().collect {
                it?.let {
                binding.fajrTime.text = CommonUtils.convertSalahTime(it.Fajr)
                binding.sunriseTime.text = CommonUtils.convertSalahTime(it.Sunrise)
                binding.dhuhrTime.text = CommonUtils.convertSalahTime(it.Dhuhr)
                binding.asrTime.text = CommonUtils.convertSalahTime(it.Asr)
                binding.maghribTime.text = CommonUtils.convertSalahTime(it.Maghrib)
                binding.ishaTime.text = CommonUtils.convertSalahTime(it.Isha)
                loadingAlert.dismissDialog()


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

                } else if (CommonUtils.getTimeLong(it.Sunrise, false) >= CommonUtils.getTimeLong(
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

                } else if (CommonUtils.getTimeLong(it.Maghrib, false) >= CommonUtils.getTimeLong(
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


            }
                 ?: run {
                binding.fajrTime.text = getString(R.string.error)
                binding.sunriseTime.text = getString(R.string.error)
                binding.dhuhrTime.text = getString(R.string.error)
                binding.asrTime.text = getString(R.string.error)
                binding.maghribTime.text = getString(R.string.error)
                binding.ishaTime.text = getString(R.string.error)
                binding.salahName.text = getString(R.string.error)
                binding.prayerTime.text = getString(R.string.error)
                binding.prayerCountdown.text = getString(R.string.error)
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



        /*lifecycleScope.launchWhenStarted {
            mainViewModel.state.collect{ islamicListState ->
                islamicListState.let { islamicInfo ->
                    islamicInfo.error.let {
                        if (it.isNotBlank()) {
                            binding.fajrTime.text = it
                            binding.sunriseTime.text = it
                            binding.dhuhrTime.text = it
                            binding.asrTime.text = it
                            binding.maghribTime.text = it
                            binding.ishaTime.text = it
                            binding.salahName.text = it
                            binding.prayerTime.text = it
                            binding.prayerCountdown.text = it
                        }
                    }
                   islamicInfo.islamicInfo.data?.let {
                       val currentTime=Constants.getCurrentTime()
                           if (Constants.getTimeLong(it.timings.Fajr,false)>=Constants.getTimeLong(currentTime,true)){
                               binding.salahName.text= getString(R.string.fajr)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Fajr)
                               binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Fajr,false)-Constants.getTimeLong(currentTime,true))
                           } else if (Constants.getTimeLong(it.timings.Sunrise,false)>=Constants.getTimeLong(currentTime,true)){
                               binding.salahName.text=getString(R.string.sunrise)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Sunrise)
                               binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Sunrise,false)-Constants.getTimeLong(currentTime,true))
                           } else if (Constants.getTimeLong(it.timings.Dhuhr,false)>=Constants.getTimeLong(currentTime,true)){
                               binding.salahName.text= getString(R.string.duhr)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Dhuhr)
                               binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Dhuhr,false)-Constants.getTimeLong(currentTime,true))
                           } else if (Constants.getTimeLong(it.timings.Asr,false)>=Constants.getTimeLong(currentTime,true)){
                               binding.salahName.text=  getString(R.string.asr)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Asr)
                               binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Asr,false)-Constants.getTimeLong(currentTime,true))
                           } else if (Constants.getTimeLong(it.timings.Maghrib,false)>=Constants.getTimeLong(currentTime,true)){
                               binding.salahName.text=getString(R.string.maghreb)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Maghrib)
                              *//* binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Maghrib)-Constants.getTimeLong(currentTime))
*//*
                               val timer = object: CountDownTimer(
                                   Constants.getTimeLong(it.timings.Maghrib,false)-Constants.getTimeLong(currentTime,true)
                                   , 1000) {
                                   override fun onTick(millisUntilFinished: Long) {
                                       binding.prayerCountdown.text=Constants.updateCountDownText(millisUntilFinished)
                                   }

                                   override fun onFinish() {

                                   }


                               }
                               timer.start()

                           } else {
                               binding.salahName.text=getString(R.string.isha)
                               binding.prayerTime.text= Constants.convertSalahTime(it.timings.Isha)
                               binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Isha,false)-Constants.getTimeLong(currentTime,true))
                           }


                        binding.fajrTime.text=Constants.convertSalahTime(it.timings.Fajr)
                        binding.sunriseTime.text=Constants.convertSalahTime(it.timings.Sunrise)
                        binding.dhuhrTime.text=Constants.convertSalahTime(it.timings.Dhuhr)
                        binding.asrTime.text=Constants.convertSalahTime(it.timings.Asr)
                        binding.maghribTime.text=Constants.convertSalahTime(it.timings.Maghrib)
                        binding.ishaTime.text=Constants.convertSalahTime(it.timings.Isha)
                    }
                }
            }


        }*/



        //<editor-fold desc="Create Text">
        val builder = StringBuilder()

       /* homeViewModel.text.observe(viewLifecycleOwner) {
            // insert  البسملة
            builder.append(it.substring(0, 10 + 1)) // +1 as substring upper bound is excluded
            builder.append(
                MessageFormat.format(
                    "{0} ﴿ {1} ﴾ ",
                    it,
                    10
                )
            )
            //</editor-fold>
            //</editor-fold>
            textView.setText(
                getSpannable(builder.toString()),
                TextView.BufferType.SPANNABLE
            )
           val typeface = Typeface.createFromAsset(App.getInstance().assets, "me_quran.ttf")
            textView.typeface = typeface

            // text justifivation

            // text justifivation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                textView.justificationMode=Layout.JUSTIFICATION_MODE_NONE
            }
        }*/


            // check if any salaet is checked

        if (sharedPreference.getString("fajr","") == "true"){
            binding.cbFajr.isChecked = true
        }
        if (sharedPreference.getString("dhuhr","") == "true"){
            binding.cbDhuhr.isChecked = true
        }
        if (sharedPreference.getString("asr","") == "true"){
            binding.cbAsr.isChecked = true
        }
        if (sharedPreference.getString("maghrib","") == "true"){
            binding.cbMaghrib.isChecked = true
        }

        if (sharedPreference.getString("isha","") == "true"){
            binding.cbIsha.isChecked = true
        }



        // adhen alarm create by fkchaou 08/03/2023



        binding.cbFajr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbFajr.isChecked){

                editor.putString("fajr","true")
                editor.commit()

                var houre = binding.fajrTime.text.toString().substring(0,2).toInt()
                var minute = binding.fajrTime.text.toString().substring(3,5).toInt()

                val alarmReceivedIntent = Intent(
                    requireContext().applicationContext,
                    MyBroadcastReceiver::class.java // this class must be inherited from BroadcastReceiver
                )



                smplrAlarmSet(requireContext().applicationContext) {
                    hour { houre }
                    min { minute }
                    requestCode { 1810 }
                    alarmReceivedIntent { alarmReceivedIntent }
                     // name of this parameter is intent in the version 2.1.0 and earlier

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()


                    }

                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.prayer_icon }


                            autoCancel { true }

                        }
                    }
                    notificationChannel {
                        channel {
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                            name { "de.coldtea.smplr.alarm.channel" }
                            description { "This notification channel is created by SmplrAlarm" }
                        }
                    }
                }
//



            }
            else{
                editor.putString("fajr","false")
                editor.commit()

                smplrAlarmCancel(requireContext().applicationContext) {
                    requestCode { 1810 }
                }


            }
        }

        binding.cbDhuhr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbDhuhr.isChecked){

                editor.putString("dhuhr","true")
                editor.commit()
                var houre = binding.dhuhrTime.text.toString().substring(0,2).toInt()
                var minute = binding.dhuhrTime.text.toString().substring(3,5).toInt()

                val alarmReceivedIntent = Intent(
                    requireContext().applicationContext,
                    MyBroadcastReceiver::class.java // this class must be inherited from BroadcastReceiver
                )



                smplrAlarmSet(requireContext().applicationContext) {
                    hour { houre }
                    min { minute }
                    requestCode { 1820 }
                    alarmReceivedIntent { alarmReceivedIntent }
                    // name of this parameter is intent in the version 2.1.0 and earlier

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()


                    }

                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.prayer_icon }


                            autoCancel { true }

                        }
                    }
                    notificationChannel {
                        channel {
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                            name { "de.coldtea.smplr.alarm.channel" }
                            description { "This notification channel is created by SmplrAlarm" }
                        }
                    }
                }
//



            }
            else {
                editor.putString("dhuhr", "false")
                editor.commit()

                smplrAlarmCancel(requireContext().applicationContext) {
                    requestCode { 1820 }
                }
            }
        }


        binding.cbAsr.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbAsr.isChecked){

                editor.putString("asr","true")
                editor.commit()

                var houre = binding.asrTime.text.toString().substring(0,2).toInt()+12
                var minute = binding.asrTime.text.toString().substring(3,5).toInt()

                val alarmReceivedIntent = Intent(
                    requireContext().applicationContext,
                    MyBroadcastReceiver::class.java // this class must be inherited from BroadcastReceiver
                )



                smplrAlarmSet(requireContext().applicationContext) {
                    hour { houre }
                    min { minute }
                    requestCode { 1830 }
                    alarmReceivedIntent { alarmReceivedIntent }
                    // name of this parameter is intent in the version 2.1.0 and earlier

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()


                    }

                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.prayer_icon }


                            autoCancel { true }

                        }
                    }
                    notificationChannel {
                        channel {
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                            name { "de.coldtea.smplr.alarm.channel" }
                            description { "This notification channel is created by SmplrAlarm" }
                        }
                    }
                }
//



            }
            else {
                editor.putString("asr", "false")
                editor.commit()

                smplrAlarmCancel(requireContext().applicationContext) {
                    requestCode { 1830 }
                }
            }
        }


        binding.cbMaghrib.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbMaghrib.isChecked){

                editor.putString("maghrib","true")
                editor.commit()
                var houre = binding.maghribTime.text.toString().substring(0,2).toInt() +12
                var minute = binding.maghribTime.text.toString().substring(3,5).toInt()
                val alarmReceivedIntent = Intent(
                    requireContext().applicationContext,
                    MyBroadcastReceiver::class.java // this class must be inherited from BroadcastReceiver
                )



                smplrAlarmSet(requireContext().applicationContext) {
                    hour { houre }
                    min { minute }
                    requestCode { 1840 }
                    alarmReceivedIntent { alarmReceivedIntent }
                    // name of this parameter is intent in the version 2.1.0 and earlier

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()


                    }

                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.prayer_icon }


                            autoCancel { true }

                        }
                    }
                    notificationChannel {
                        channel {
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                            name { "de.coldtea.smplr.alarm.channel" }
                            description { "This notification channel is created by SmplrAlarm" }
                        }
                    }
                }
//



            }
            else {
                editor.putString("maghrib", "false")
                editor.commit()

                smplrAlarmCancel(requireContext().applicationContext) {
                    requestCode { 1840 }
                }
            }
        }


        binding.cbIsha.setOnCheckedChangeListener { compoundButton, b ->
            if(binding.cbIsha.isChecked){

                editor.putString("isha","true")
                editor.commit()
                var houre = binding.ishaTime.text.toString().substring(0,2).toInt() +12
                var minute = binding.ishaTime.text.toString().substring(3,5).toInt()

                val alarmReceivedIntent = Intent(
                    requireContext().applicationContext,
                    MyBroadcastReceiver::class.java // this class must be inherited from BroadcastReceiver
                )



                smplrAlarmSet(requireContext().applicationContext) {
                    hour { houre }
                    min { minute }
                    requestCode { 1850 }
                    alarmReceivedIntent { alarmReceivedIntent }
                    // name of this parameter is intent in the version 2.1.0 and earlier

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()


                    }

                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.prayer_icon }


                            autoCancel { true }

                        }
                    }
                    notificationChannel {
                        channel {
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                            name { "de.coldtea.smplr.alarm.channel" }
                            description { "This notification channel is created by SmplrAlarm" }
                        }
                    }
                }
//



            }
            else {
                editor.putString("isha", "false")
                editor.commit()

                smplrAlarmCancel(requireContext().applicationContext) {
                    requestCode { 1850 }
                }
            }

        }



        binding.update.setOnClickListener {

            if (sharedPreference.getString("fajr","") == "true"){

                var houre = binding.fajrTime.text.toString().substring(0,2).toInt()
                var minute = binding.fajrTime.text.toString().substring(3,5).toInt()

                smplrAlarmUpdate(requireContext().applicationContext) {
                    requestCode { 1810 }
                    hour { houre }
                    min { minute }



                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()

                    }

                }



            }


            if (sharedPreference.getString("asr","") == "true"){


                var houre = binding.asrTime.text.toString().substring(0,2).toInt() +12
                var minute = binding.asrTime.text.toString().substring(3,5).toInt()
                smplrAlarmUpdate(requireContext().applicationContext) {
                    requestCode { 1830 }
                    hour { houre }
                    min { minute }

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()

                    }

                }


            }

            if (sharedPreference.getString("dhuhr","") == "true"){


                editor.putString("dhuhr","true")
                editor.commit()
                var houre = binding.dhuhrTime.text.toString().substring(0,2).toInt()
                var minute = binding.dhuhrTime.text.toString().substring(3,5).toInt()
                smplrAlarmUpdate(requireContext().applicationContext) {
                    requestCode { 1820 }
                    hour { houre }
                    min { minute }

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()

                    }

                }

            }


            if (sharedPreference.getString("maghrib","") == "true"){
                var houre = binding.maghribTime.text.toString().substring(0,2).toInt() +12
                var minute = binding.maghribTime.text.toString().substring(3,5).toInt()

                smplrAlarmUpdate(requireContext().applicationContext) {
                    requestCode { 1840 }
                    hour { houre }
                    min { minute }

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()

                    }

                }


            }

            if (sharedPreference.getString("isha","") == "true"){
                var houre = binding.ishaTime.text.toString().substring(0,2).toInt() +12
                var minute = binding.ishaTime.text.toString().substring(3,5).toInt()

                smplrAlarmUpdate(requireContext().applicationContext) {
                    requestCode { 1850 }
                    hour { houre }
                    min { minute }

                    weekdays {
                        monday()
                        friday()
                        sunday()
                        thursday()
                        tuesday()
                        wednesday()
                        saturday()

                    }

                }




            }

            Toast.makeText(requireContext(), "تم تحديث أوقات الصلاة", Toast.LENGTH_LONG).show()
        }


        binding.qibla.setOnClickListener {

//            val intent = Intent(requireContext(), QiblaActivity::class.java)
//            requireContext().startActivity(intent)

            val bundle = Bundle()
            bundle.putString("altitude", lastLocation?.altitude.toString())
            bundle.putString("longitude", lastLocation?.longitude.toString())

            findNavController().navigate(R.id.action_navigation_home_to_qiblaFragment2, bundle)



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



    fun setDataView(prayerName: String, prayerTime: String, time:Long,countDown:Boolean) {
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

            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null)

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


    private fun statusLocationCheck() {
        val manager = requireActivity().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()

        }else{
            checkLocationPermission()
        }
    }


    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(getString(R.string.openLocation))
            .setCancelable(false)
            .setPositiveButton(
                getString(R.string.openSettings)
            ) { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
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