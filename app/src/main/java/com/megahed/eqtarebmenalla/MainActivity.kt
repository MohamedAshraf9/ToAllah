package com.megahed.eqtarebmenalla

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.databinding.ActivityMainBinding
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.quran.QuranData
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    //private lateinit var fusedLocationClient: FusedLocationProviderClient



    override fun onCreate(savedInstanceState: Bundle?) {
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchNotificationPermission()
        }

       /* val mainViewModel = ViewModelProvider(this).get(IslamicViewModel::class.java)*/
        //val prayerTimeViewModel = ViewModelProvider(this).get(PrayerTimeViewModel::class.java)







       /* val fileInString1: String =
            applicationContext.assets.open("ar_translation_2.json").bufferedReader().use {
                //Log.d("MyTagData",it.readText())
                it.readText()
            }
        val jsonObject=JSONObject(fileInString1)
        val jsonArray=jsonObject.getJSONObject("verse")
        val jsonArray1=jsonObject.getInt("count")
        for (i in 0 until jsonArray1) {
            val s="verse_${i+1}"
            Log.d("MyTagData", jsonArray.get(s).toString())
        }*/


       /* val fileInString1: String =
            applicationContext.assets.open("quran.json").bufferedReader().use {
                //Log.d("MyTagData",it.readText())
                it.readText()
            }
        val jsonObject=JSONObject(fileInString1)
        val jsonArray=jsonObject.getJSONArray("surahs")
        for (i in 0 until jsonArray.length()){
            Log.d("MyTagData", jsonArray.getJSONObject(i).getString("name"))
            val ayat=jsonArray.getJSONObject(i).getJSONArray("ayahs")
            for (j in 0 until ayat.length()){
                Log.d("MyTagData", ayat.getJSONObject(j).getString("text"))
            }

            //Toast.makeText(this,jsonArray.getJSONObject(i).getString("name"),Toast.LENGTH_LONG).show()
        }*/



     /*   val fileInString: String =
            applicationContext.assets.open("quran.json").bufferedReader().use {
                //Log.d("MyTagData",it.readText())
                it.readText()
            }
       val data= Gson().fromJson(fileInString,QuranTextDto::class.java)
        for (i in 0 until data.surahs.size){
            Log.d("MyTagData", data.surahs[i].name)
        }*/

       /* val fileInString: String =
            applicationContext.assets.open("surah.json").bufferedReader().use {
                //Log.d("MyTagData",it.readText())
                it.readText()
            }
        val data= Gson().fromJson(fileInString,QuranData::class.java)
        Log.d("MyTagData",""+ data.size)
        for (i in 0 until data.size){
            Log.d("MyTagData", data[i].title)
            Log.d("MyTagData", data[i].titleAr)
        }

*/



        /*fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                location?.let {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address> =
                        geocoder.getFromLocation(it.latitude, it.longitude, 1) as List<Address>
                    val cityName: String = addresses[0].getAddressLine(0)
                    Log.d("MyTag", "\n====================================================\n")
                    Log.d("MyTag", "cityName : $cityName  addressesNum= ${addresses.size}")
                    Toast.makeText(this,"cityName : $cityName  addressesNum= ${addresses.size}",Toast.LENGTH_LONG).show()
                }
            }*/





       /* val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise location access granted.
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location : Location? ->
                            // Got last known location. In some rare situations this can be null.
                            location?.let {
                                val geocoder = Geocoder(this, Locale.getDefault())
                                val addresses: List<Address> = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                val cityName: String = addresses[0].getAddressLine(0)
                                Log.d("MyTag", "\n====================================================\n")
                                Log.d("MyTag", "cityName : $cityName   addressesNum= ${addresses.size}")
                                Toast.makeText(this,"cityName : $cityName   addressesNum= ${addresses.size}",Toast.LENGTH_LONG).show()
                            }
                        }
                    fusedLocationClient.locationAvailability
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
            }
            }
        }*/

// ...

// Before you perform the actual permission request, check whether your app
// already has the permissions, and whether your app needs to show a permission
// rationale dialog. For more details, see Request permissions.
       /* locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))*/



        val navView: BottomNavigationView = binding.navView




        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_quran, R.id.navigation_listener
            )
        )
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("LOG", "Permission granted")
                true
            } else {
                Log.v("LOG", "Permission revoked")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else {
            Log.v("LOG", "Permission is granted")
            true
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

}