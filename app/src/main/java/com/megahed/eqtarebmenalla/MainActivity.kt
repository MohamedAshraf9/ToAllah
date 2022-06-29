package com.megahed.eqtarebmenalla

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.megahed.eqtarebmenalla.databinding.ActivityMainBinding
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.AzanViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: AzanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel=  ViewModelProvider(this).get(AzanViewModel::class.java)

        val navView: BottomNavigationView = binding.navView


        mainViewModel.getAzanData("cairo","egypt")
        lifecycleScope.launchWhenCreated {

            mainViewModel.state.collect{
                it.azanInfoDto.forEach {
                    Log.d("MyDataInfo",it.data.timings.Asr)
                    Log.d("MyDataInfo",it.data.timings.Dhuhr)
                    Log.d("MyDataInfo",it.data.timings.Fajr)
                    Log.d("MyDataInfo",it.data.timings.Isha)
                    Log.d("MyDataInfo",it.data.timings.Maghrib)
                    Log.d("MyDataInfo",it.data.timings.Sunset)
                    Log.d("MyDataInfo",it.data.timings.Sunrise)
                    Log.d("MyDataInfo",it.data.timings.Dhuhr)
                }
            }
        }

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}