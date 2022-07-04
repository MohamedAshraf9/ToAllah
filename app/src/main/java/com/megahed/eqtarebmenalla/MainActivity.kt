package com.megahed.eqtarebmenalla

import android.Manifest
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.databinding.ActivityMainBinding
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.quran.QuranData
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainViewModel = ViewModelProvider(this).get(IslamicViewModel::class.java)
        val prayerTimeViewModel = ViewModelProvider(this).get(PrayerTimeViewModel::class.java)

        mainViewModel.getAzanData("Cairo","Egypt")
        lifecycleScope.launchWhenStarted {
            mainViewModel.state.collect{ islamicListState ->
                islamicListState.let { islamicInfo ->
                    islamicInfo.islamicInfo.data?.let {
                        val prayerTime=PrayerTime(1,it.date.gregorian.date,it.timings.Asr,it.timings.Dhuhr,
                            it.timings.Fajr,it.timings.Isha,it.timings.Maghrib,it.timings.Sunrise)
                        prayerTimeViewModel.insertPrayerTime(prayerTime)

                    }
                }
            }


        }



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



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                location?.let {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address> = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    val cityName: String = addresses[0].getAddressLine(0)
                    Log.d("MyTag", "\n====================================================\n")
                    Log.d("MyTag", "cityName : $cityName  addressesNum= ${addresses.size}")
                    Toast.makeText(this,"cityName : $cityName  addressesNum= ${addresses.size}",Toast.LENGTH_LONG).show()
                }
            }


        val locationPermissionRequest = registerForActivityResult(
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
        }

// ...

// Before you perform the actual permission request, check whether your app
// already has the permissions, and whether your app needs to show a permission
// rationale dialog. For more details, see Request permissions.
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))



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
}