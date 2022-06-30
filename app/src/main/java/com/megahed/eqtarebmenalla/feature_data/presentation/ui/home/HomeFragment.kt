package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
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
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentHomeBinding
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.*
import java.util.regex.Pattern

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainViewModel =
            ViewModelProvider(this).get(IslamicViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

       binding.dayDetails.text= DateFormat.getDateInstance(DateFormat.FULL).format(Date())

        mainViewModel.getAzanData("Cairo","Egypt")
        lifecycleScope.launchWhenStarted {
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
                       binding.dayDetailsHijri.text="${it.date.hijri.day} ${it.date.hijri.month.ar} ${it.date.hijri.year} "
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
                              /* binding.prayerCountdown.text=Constants.updateCountDownText(
                                   Constants.getTimeLong(it.timings.Maghrib)-Constants.getTimeLong(currentTime))
*/
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


        }



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
        _binding = null
    }
}