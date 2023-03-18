package com.megahed.eqtarebmenalla.feature_data.presentation.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.FragmentQiblaBinding
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.QiblaVM
import org.json.JSONObject
import java.io.IOException


class QiblaFragment : Fragment(), SensorEventListener , MenuProvider {

    lateinit var qiblaVM: QiblaVM
    lateinit var binding : FragmentQiblaBinding
    lateinit var sensorManager : SensorManager
    lateinit var sensor: Sensor
    var altitude = ""
    var longitude =""
    var angl = 0
    var curent_degree =0F
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View{
        binding = FragmentQiblaBinding.inflate(inflater, container, false)
        val root : View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title=" "
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val tm = getSystemService(requireContext(), TelephonyManager::class.java) as TelephonyManager
        val locale = tm.networkCountryIso

        val jObj = JSONObject(getJsonDataFromAsset("location.json", requireContext()))
        val jsonArry = jObj.getJSONArray("ref_country_codes")
        for (i in 0 until jsonArry.length()) {
            val user = HashMap<String, String?>()
            val obj = jsonArry.getJSONObject(i)
            if(obj.getString("alpha2").toString().lowercase() == locale){
               altitude = obj.getString("latitude")
                longitude = obj.getString("longitude")
            }

        }

        qiblaVM = QiblaVM()

        qiblaVM.getQibla(altitude, longitude).observe(viewLifecycleOwner, Observer {
            angl = it.data.direction.toInt()
        })

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager


        return root
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val type = event!!.sensor.type
        val data: FloatArray

       var degree = Math.round(event.values[0]).toFloat()
        var rotateAnimation = RotateAnimation(
            curent_degree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F)

        rotateAnimation.duration = 100
        binding.mainImageDial.startAnimation(rotateAnimation)
//        rotateAnimation.fillAfter = true
        Log.println(Log.ASSERT, "qibla degr", degree.toString())
        binding.sotwLabel.text = degree.toString()
        if (degree.toInt() in angl-5 .. angl+5){
            binding.constraint.setBackgroundColor(requireContext().getColor(R.color.primary_color))
        }
        else{
            binding.constraint.setBackgroundColor(requireContext().getColor(R.color.background_color))

        }
       curent_degree = -degree
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    fun getJsonDataFromAsset(fileName: String, context : Context): String? {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return  when (menuItem.itemId) {
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
            }
            else -> false
        }
    }



}