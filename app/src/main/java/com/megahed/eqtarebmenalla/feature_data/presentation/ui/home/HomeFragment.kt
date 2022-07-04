package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentHomeBinding
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.PrayerTimeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern

@AndroidEntryPoint
class HomeFragment : Fragment() {




    private lateinit var binding: FragmentHomeBinding
    private var timeStarted : Long = 0
    private var timeElapsed : Long = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainViewModel =
            ViewModelProvider(this).get(PrayerTimeViewModel::class.java)

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

       binding.dayDetails.text= DateFormat.getDateInstance(DateFormat.FULL).format(Date())



        lifecycleScope.launchWhenStarted {
            mainViewModel.getPrayerTimeById()?.let {
                binding.fajrTime.text=CommonUtils.convertSalahTime(it.Fajr)
                binding.sunriseTime.text=CommonUtils.convertSalahTime(it.Sunrise)
                binding.dhuhrTime.text=CommonUtils.convertSalahTime(it.Dhuhr)
                binding.asrTime.text=CommonUtils.convertSalahTime(it.Asr)
                binding.maghribTime.text=CommonUtils.convertSalahTime(it.Maghrib)
                binding.ishaTime.text=CommonUtils.convertSalahTime(it.Isha)


                val currentTime=CommonUtils.getCurrentTime()
                if (CommonUtils.getTimeLong(it.Fajr,false)>=CommonUtils.getTimeLong(currentTime,true)){

                    setDataView(getString(R.string.fajr),CommonUtils.convertSalahTime(it.Fajr),
                        CommonUtils.getTimeLong(it.Fajr,false)-CommonUtils.getTimeLong(currentTime,true),true)

                } else if (CommonUtils.getTimeLong(it.Sunrise,false)>=CommonUtils.getTimeLong(currentTime,true)){
                    setDataView(getString(R.string.sunrise),CommonUtils.convertSalahTime(it.Sunrise),
                        CommonUtils.getTimeLong(it.Sunrise,false)-CommonUtils.getTimeLong(currentTime,true),true)

                } else if (CommonUtils.getTimeLong(it.Dhuhr,false)>=CommonUtils.getTimeLong(currentTime,true)){

                    setDataView(getString(R.string.duhr),CommonUtils.convertSalahTime(it.Dhuhr),
                        CommonUtils.getTimeLong(it.Dhuhr,false)-CommonUtils.getTimeLong(currentTime,true),true)

                } else if (CommonUtils.getTimeLong(it.Asr,false)>=CommonUtils.getTimeLong(currentTime,true)){

                    setDataView(getString(R.string.asr),CommonUtils.convertSalahTime(it.Asr),
                        CommonUtils.getTimeLong(it.Asr,false)-CommonUtils.getTimeLong(currentTime,true),true)

                } else if (CommonUtils.getTimeLong(it.Maghrib,false)>=CommonUtils.getTimeLong(currentTime,true)){
                    setDataView(getString(R.string.maghreb),CommonUtils.convertSalahTime(it.Maghrib),
                        CommonUtils.getTimeLong(it.Maghrib,false)-CommonUtils.getTimeLong(currentTime,true),true)


                } else if (CommonUtils.getTimeLong(it.Isha,false)>=CommonUtils.getTimeLong(currentTime,true)) {
                    setDataView(getString(R.string.isha),CommonUtils.convertSalahTime(it.Isha),
                        CommonUtils.getTimeLong(it.Isha,false)-CommonUtils.getTimeLong(currentTime,true),true)
                }
                else{
                    setDataView(getString(R.string.isha),CommonUtils.convertSalahTime(it.Isha),
                        0,false)

                    timeStarted=CommonUtils.getTimeLong(it.Isha,false)
                    startCoroutineTimer()

                }



            }?:run{
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
        return root
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


}