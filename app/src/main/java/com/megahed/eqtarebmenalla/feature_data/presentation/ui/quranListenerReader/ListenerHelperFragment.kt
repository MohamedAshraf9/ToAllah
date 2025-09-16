package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentListenerHelperBinding
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.RecitersVerse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.findNavController
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.db.model.getTotalVerses
import com.megahed.eqtarebmenalla.feature_data.data.repository.DailyTargetProgress
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.HefzViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import com.megahed.eqtarebmenalla.offline.NetworkStateObserver
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import jakarta.inject.Inject

@AndroidEntryPoint
class ListenerHelperFragment : Fragment(), MenuProvider {

    private lateinit var offlinePreferences: SharedPreferences
    private lateinit var binding: FragmentListenerHelperBinding
    var selectedSurahName: String = ""
    var soraNumbers = arrayListOf<Int>()
    var readers = mutableListOf<RecitersVerse>()
    var soraId: Int = 0
    var startAya: Int = 0
    var endAya: Int = 0
    var reader: RecitersVerse? = null
    private val hefzViewModel: HefzViewModel by activityViewModels()
    private val memorizationViewModel: MemorizationViewModel by viewModels()
    private var networkStateObserver: NetworkStateObserver? = null

    @Inject
    lateinit var offlineAudioManager: OfflineAudioManager

    var job: Job? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =
            requireActivity().getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentListenerHelperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offlinePreferences =
            requireActivity().getSharedPreferences("offline_prefs", Context.MODE_PRIVATE)
        setupToolbar()
        setupListeners()
        setupObservers()
        hefzViewModel.refreshReciters()
        setupMemorizationScheduleCard()
        loadScheduleProgress()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = getString(R.string.listeningToSave)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupListeners() {
        binding.listOfRewat.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                binding.listSouraName.setText("")
                binding.nbAya.setText("")
                binding.nbEyaEnd.setText("")
                binding.start.isEnabled = false
                reader = parent.getItemAtPosition(position) as RecitersVerse
                binding.soraStartSpinner.isEnabled = true
            }

        binding.listSouraName.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                binding.nbAya.setText("")
                binding.nbEyaEnd.setText("")
                binding.start.isEnabled = false

                val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
                val selectedSurahName = surahNames[position]
                soraId = position + 1

                setupVerseSelection(selectedSurahName)
            }
        binding.btnMemorizationTracker.setOnClickListener {
            if (reader == null) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.select_reader_first_tracker),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val todayTarget = memorizationViewModel.todayTarget.value
            if (todayTarget != null) {
                autoFillWithTodayTarget(todayTarget)

                if (!isNetworkAvailable()) {
                    disableFieldsForOffline()
                }

                Snackbar.make(
                    binding.root,
                    getString(R.string.filled_with_today_target),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.no_today_target),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        binding.nbAya.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            startAya = parent.getItemAtPosition(position) as Int
            binding.soraStartEndText.isEnabled = true
        }

        binding.nbEyaEnd.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            endAya = parent.getItemAtPosition(position) as Int
            binding.start.isEnabled = true
        }

        binding.stop.setOnClickListener {
            binding.start.visibility = View.VISIBLE
            binding.stop.visibility = View.GONE
        }

        binding.btnCreateSchedule.setOnClickListener {
            requireView().findNavController()
                .navigate(R.id.action_listenerHelperFragment_to_scheduleCreationFragment)
        }

        binding.btnMemorizationTracker.setOnClickListener {
            if (!isNetworkAvailable()) {
                val todayTarget = memorizationViewModel.todayTarget.value
                if (todayTarget != null) {
                    autoFillWithTodayTargetOffline(todayTarget)
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.no_today_target),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                if (reader == null) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.select_reader_first_tracker),
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val todayTarget = memorizationViewModel.todayTarget.value
                if (todayTarget != null) {
                    autoFillWithTodayTarget(todayTarget)
                    Snackbar.make(
                        binding.root,
                        getString(R.string.filled_with_today_target),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.no_today_target),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.start.setOnClickListener {
            handleStartMemorization()
        }

        binding.memorizationScheduleCard.setOnClickListener {
            requireView().findNavController()
                .navigate(R.id.action_listenerHelperFragment_to_scheduleCreationFragment)
        }
    }

    private fun disableFieldsForOffline() {
        binding.listSouraName.isEnabled = false
        binding.nbAya.isEnabled = false
        binding.nbEyaEnd.isEnabled = false
    }

    private fun autoFillWithTodayTarget(target: DailyTarget) {
        val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
        val surahIndex = surahNames.indexOf(target.surahName)

        if (surahIndex >= 0) {
            binding.listSouraName.setText(target.surahName, false)
            soraId = target.surahId
            selectedSurahName = target.surahName

            val soraNumber = Constants.SORA_OF_QURAN_WITH_NB_EYA[target.surahName] ?: 0
            soraNumbers.clear()
            for (i in 1..soraNumber) {
                soraNumbers.add(i)
            }

            val verseAdapter =
                ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
            verseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.nbAya.setAdapter(verseAdapter)
            binding.nbEyaEnd.setAdapter(verseAdapter)

            val remainingStartVerse = if (target.completedVerses > 0) {
                target.startVerse + target.completedVerses
            } else {
                target.startVerse
            }

            val actualStartVerse = minOf(remainingStartVerse, target.endVerse)

            binding.nbAya.setText(actualStartVerse.toString(), false)
            binding.nbEyaEnd.setText(target.endVerse.toString(), false)

            startAya = actualStartVerse
            endAya = target.endVerse

            binding.soraStartSpinner.isEnabled = true
            binding.soraStartEditText.isEnabled = true
            binding.soraStartEndText.isEnabled = true
            binding.start.isEnabled = reader != null

            val remainingVerses = endAya - startAya + 1
            if (target.completedVerses > 0) {
                Snackbar.make(
                    binding.root,
                    "تم ملء البيانات للآيات المتبقية: $remainingVerses آية (من $actualStartVerse إلى ${target.endVerse})",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupVerseSelection(selectedSurahName: String) {
        val soraNumber = Constants.SORA_OF_QURAN_WITH_NB_EYA[selectedSurahName] ?: 0
        soraNumbers.clear()
        for (i in 1..soraNumber) {
            soraNumbers.add(i)
        }

        val verseAdapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
        verseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.nbAya.setAdapter(verseAdapter)
        binding.nbEyaEnd.setAdapter(verseAdapter)

        binding.soraStartEditText.isEnabled = true
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }


    private fun startMemorization() {
        val ayaR = binding.nbAyaRepeat.text.toString()
        val soraR = binding.suraRepeat.text.toString()

        if (ayaR.trim().isEmpty() || soraR.trim().isEmpty()) {
            MethodHelper.toastMessage(getString(R.string.addValidData))
            return
        }

        if (startAya > endAya) {
            MethodHelper.toastMessage(getString(R.string.ayaWrong))
            return
        }

        reader?.let { selectedReader ->
            val baseUrl = when {
                selectedReader.audio_url_bit_rate_128.trim()
                    .isNotEmpty() && selectedReader.audio_url_bit_rate_128.trim() != "0" ->
                    selectedReader.audio_url_bit_rate_128

                selectedReader.audio_url_bit_rate_64.trim()
                    .isNotEmpty() && selectedReader.audio_url_bit_rate_64.trim() != "0" ->
                    selectedReader.audio_url_bit_rate_64

                else -> selectedReader.audio_url_bit_rate_32_
            }

            sharedPreferences.edit {
                putBoolean("isPlayingSora", false)
                putInt("repeat_count", ayaR.toInt())
                putInt("all_repeat", soraR.toInt())
            }

            val action: NavDirections = ListenerHelperFragmentDirections
                .actionListenerHelperFragmentToHefzRepeatActivity(
                    link = baseUrl,
                    soraId = soraId.toString(),
                    startAya = startAya.toString(),
                    endAya = endAya.toString(),
                    ayaRepeat = ayaR.toInt(),
                    allRepeat = soraR.toInt(),
                    readerName = selectedReader.name,
                    readerId = selectedReader.id.toString()
                )
            requireView().findNavController().navigate(action)
        }
    }

    private fun setupObservers() {
        job = lifecycleScope.launch {
            hefzViewModel.state.collect { ayaHefzState ->
                if (isNetworkAvailable()) {
                    val filter = ayaHefzState.recitersVerse.filter {
                        (it.audio_url_bit_rate_32_.trim()
                            .isNotEmpty() && it.audio_url_bit_rate_32_.trim() != "0") ||
                                (it.audio_url_bit_rate_64.trim()
                                    .isNotEmpty() && it.audio_url_bit_rate_64.trim() != "0") ||
                                (it.audio_url_bit_rate_128.trim()
                                    .isNotEmpty() && it.audio_url_bit_rate_128.trim() != "0")
                    }
                    readers.clear()
                    readers.addAll(filter)

                    enableAllFields(true)
                } else {
                    readers.clear()
                    val selectedReaderId =
                        offlinePreferences.getString("selected_offline_reader_id", null)
                    val selectedReaderName =
                        offlinePreferences.getString("selected_offline_reader_name", null)

                    if (selectedReaderId != null && selectedReaderName != null) {
                        val offlineReciter = RecitersVerse(
                            id = selectedReaderId,
                            name = selectedReaderName,
                            audio_url_bit_rate_128 = offlinePreferences.getString(
                                "selected_offline_reader_url_128",
                                ""
                            ) ?: "",
                            audio_url_bit_rate_64 = offlinePreferences.getString(
                                "selected_offline_reader_url_64",
                                ""
                            ) ?: "",
                            audio_url_bit_rate_32_ = offlinePreferences.getString(
                                "selected_offline_reader_url_32",
                                ""
                            ) ?: "",
                            musshaf_type = "",
                            rewaya = ""
                        )
                        readers.add(offlineReciter)
                        reader = offlineReciter

                        if (isAdded && view != null) {
                            val todayTarget = memorizationViewModel.todayTarget.value
                            if (todayTarget != null) {
                                autoFillWithTodayTargetOffline(todayTarget)
                            }
                        }


                        enableAllFields(false)
                    }
                }

                if (readers.isNotEmpty()) {
                    val adapter =
                        ArrayAdapter(requireContext(), R.layout.list_item_spinner, readers)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.listOfRewat.setAdapter(adapter)

                    if (!isNetworkAvailable() && readers.isNotEmpty()) {
                        binding.listOfRewat.setText(readers[0].name, false)
                    }
                } else if (!isNetworkAvailable()) {
                    Snackbar.make(
                        binding.root,
                        "لا يوجد قراء متاحين بدون إتصال في الوقت الحالي.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
        lifecycleScope.launch {
            memorizationViewModel.currentSchedule.collectLatest { schedule ->
                if (schedule != null) {
                    binding.memorizationScheduleCard.visibility = View.VISIBLE
                    binding.btnMemorizationTracker.visibility = View.VISIBLE
                    binding.scheduleTitle.text = schedule.title
                    loadScheduleProgress()
                    lifecycleScope.launch {
                        val isCompleted = checkIfScheduleCompleted(schedule)
                        updateCreateScheduleButton(schedule, isCompleted)
                    }
                } else {
                    binding.memorizationScheduleCard.visibility = View.GONE
                    binding.btnMemorizationTracker.visibility = View.GONE
                    binding.btnCreateSchedule.text = getString(R.string.create_schedule)
                    binding.btnCreateSchedule.icon =
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_add)
                    binding.btnCreateSchedule.setOnClickListener {
                        requireView().findNavController()
                            .navigate(R.id.action_listenerHelperFragment_to_scheduleCreationFragment)
                    }
                    clearFormData()
                }
            }
        }
        lifecycleScope.launch {
            memorizationViewModel.todayTargetProgress.collectLatest { targetProgress ->
                targetProgress?.let { progress ->
                    updateTodayProgressWithPartial(progress)
                    updateScheduleCardWithProgress(progress)
                }
            }
        }

        lifecycleScope.launch {
            memorizationViewModel.todayTarget.collectLatest { target ->
                if (target != null && memorizationViewModel.todayTargetProgress.value == null) {
                    updateTodayProgress(target)
                    updateScheduleCardWithTarget(target)
                    if(!isNetworkAvailable()){
                        autoFillWithTodayTargetOffline(target)
                    }

                } else if (target == null) {
                    binding.todayProgress.text = "0/0"
                    binding.scheduleTitle.text = "لا يوجد هدف لليوم"
                }
            }
        }

        lifecycleScope.launch {
            memorizationViewModel.userStreak.collectLatest { streak ->
                binding.currentStreak.text = (streak?.currentStreak ?: 0).toString()
            }
        }

        lifecycleScope.launch {
            memorizationViewModel.uiState.collectLatest { state ->
                if (state.verseProgress != null) {
                    val verseProgress = state.verseProgress
                    val verseDetails =
                        "${verseProgress.completedVerses}/${verseProgress.totalVerses}"
                    val percentageText = "${verseProgress.progressPercentage}%"
                    binding.totalProgress.text = percentageText
                    binding.totalProgressOfSchedule.text = verseDetails
                } else if (state.scheduleProgress != null) {
                    val targetProgress = state.scheduleProgress
                    val percentageText = "${targetProgress.progressPercentage}%"
                    binding.totalProgress.text = percentageText
                } else {
                    binding.totalProgress.text = "0%"
                }

                val currentSchedule = memorizationViewModel.currentSchedule.value
                if (currentSchedule != null) {
                    lifecycleScope.launch {
                        val isCompleted = checkIfScheduleCompleted(currentSchedule)
                        updateCreateScheduleButton(currentSchedule, isCompleted)
                    }
                }
            }
        }

    }

    private fun updateTodayProgressWithPartial(targetProgress: DailyTargetProgress) {
        val completedText = "${targetProgress.completedVerses}/${targetProgress.totalVerses}"
        binding.todayProgress.text = completedText

        when {
            targetProgress.isCompleted -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }

            targetProgress.completedVerses > 0 -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }

            else -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScheduleCardWithProgress(targetProgress: DailyTargetProgress) {
        val target = targetProgress.target
        val progressIndicator = when {
            target.isCompleted -> " ✓ (مكتمل)"
            targetProgress.completedVerses > 0 -> " (${targetProgress.completedVerses}/${targetProgress.totalVerses} آية)"
            else -> ""
        }

        binding.scheduleTitle.text =
            "${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}$progressIndicator"
    }

    private fun updateCreateScheduleButton(schedule: MemorizationSchedule, isCompleted: Boolean) {
        if (isCompleted) {
            binding.btnCreateSchedule.text = getString(R.string.create_schedule)
            binding.btnCreateSchedule.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_add)
            binding.btnCreateSchedule.setOnClickListener {
                requireView().findNavController()
                    .navigate(R.id.action_listenerHelperFragment_to_scheduleCreationFragment)
            }
        } else {
            binding.btnCreateSchedule.text = "تعديل الجدول"
            binding.btnCreateSchedule.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)
            binding.btnCreateSchedule.setOnClickListener {
                val action = ListenerHelperFragmentDirections
                    .actionListenerHelperFragmentToScheduleCreationFragment(schedule.id)
                requireView().findNavController().navigate(action)
            }
        }
    }

    private fun enableAllFields(enable: Boolean) {
        binding.listOfRewat.isEnabled = enable
        binding.listSouraName.isEnabled = enable
        binding.nbAya.isEnabled = enable
        binding.nbEyaEnd.isEnabled = enable
        binding.soraStartSpinner.isEnabled = enable
        binding.soraStartEditText.isEnabled = enable
        binding.soraStartEndText.isEnabled = enable

        val alpha = if (enable) 1.0f else 0.6f
        binding.listOfRewat.alpha = alpha
        binding.listSouraName.alpha = alpha
        binding.nbAya.alpha = alpha
        binding.nbEyaEnd.alpha = alpha
    }

    private fun autoFillWithTodayTargetOffline(target: DailyTarget) {
        val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
        val surahIndex = surahNames.indexOf(target.surahName)

        if (surahIndex >= 0) {
            binding.listSouraName.setText(target.surahName, false)
            soraId = target.surahId
            selectedSurahName = target.surahName

            val soraNumber = Constants.SORA_OF_QURAN_WITH_NB_EYA[target.surahName] ?: 0
            soraNumbers.clear()
            for (i in 1..soraNumber) {
                soraNumbers.add(i)
            }

            val verseAdapter =
                ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
            verseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.nbAya.setAdapter(verseAdapter)
            binding.nbEyaEnd.setAdapter(verseAdapter)

            val remainingStartVerse = if (target.completedVerses > 0) {
                target.startVerse + target.completedVerses
            } else {
                target.startVerse
            }

            val actualStartVerse = minOf(remainingStartVerse, target.endVerse)

            binding.nbAya.setText(actualStartVerse.toString(), false)
            binding.nbEyaEnd.setText(target.endVerse.toString(), false)

            startAya = actualStartVerse
            endAya = target.endVerse

            binding.start.isEnabled = true

            if (isAdded && view != null) {
                val remainingVerses = endAya - startAya + 1
                val modeMessage = if (target.completedVerses > 0) {
                    "تم ملء البيانات للآيات المتبقية: $remainingVerses آية (وضع غير متصل)"
                } else {
                    "تم ملء البيانات تلقائياً من جدول اليوم (وضع غير متصل)"
                }

                Snackbar.make(binding.root, modeMessage, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showNoOfflineReciterDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("لا يوجد قارئ بدون اتصال")
            .setMessage("لم تقم بتحميل أي قارئ للاستماع بدون اتصال. يرجى الاتصال بالإنترنت وتحميل قارئ أولاً.")
            .setPositiveButton("موافق") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private suspend fun checkIfScheduleCompleted(schedule: MemorizationSchedule): Boolean {
        return try {
            val progress = memorizationViewModel.getScheduleProgress(schedule.id)
            progress.progressPercentage >= 100
        } catch (e: Exception) {
            false
        }
    }

    private fun handleStartMemorization() {
        if (reader == null) {
            Snackbar.make(
                binding.root,
                getString(R.string.select_reader_first),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        if (!isNetworkAvailable()) {
            lifecycleScope.launch {
                val hasOfflineData = checkOfflineDataAvailability()
                if (!hasOfflineData) {
                    showNoNetworkDialog()
                    return@launch
                }
                startMemorization()
            }
        } else {
            startMemorization()
        }
    }

    private suspend fun checkOfflineDataAvailability(): Boolean {
        return reader?.let { selectedReader ->
            for (verseId in startAya..endAya) {
                val offlineUrl = offlineAudioManager.getOfflineAudioUrl(
                    readerId = selectedReader.id,
                    surahId = soraId,
                    verseId = verseId
                )
                if (offlineUrl != null) {
                    return true
                }
            }

            val surahOfflineUrl = offlineAudioManager.getOfflineAudioUrl(
                readerId = selectedReader.id,
                surahId = soraId,
                verseId = null
            )

            val hasData = surahOfflineUrl != null
            hasData
        } ?: false
    }

    private fun showNoNetworkDialog() {
        val readerName = reader?.name ?: "القارئ المحدد"
        val surahName = selectedSurahName.ifEmpty { "السورة المحددة" }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("المحتوى غير متوفر دون اتصال")
            .setMessage("$readerName - $surahName (الآيات $startAya-$endAya)\n\nهذا المحتوى غير متوفر للوضع غير المتصل. يرجى الاتصال بالإنترنت أو اختيار محتوى آخر متوفر دون اتصال.")
            .setPositiveButton("إعدادات الاتصال") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Snackbar.make(binding.root, "لا يمكن فتح الإعدادات", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .setNeutralButton("اختيار محتوى آخر") { _, _ ->
                binding.listSouraName.setText("")
                binding.nbAya.setText("")
                binding.nbEyaEnd.setText("")
                binding.start.isEnabled = false
            }
            .setNegativeButton("إلغاء") { _, _ -> }
            .show()
    }

    private fun setupMemorizationScheduleCard() {
        val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
        val surahAdapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, surahNames)
        surahAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.listSouraName.setAdapter(surahAdapter)
        binding.btnMemorizationTracker.isEnabled = true
    }

    private fun loadScheduleProgress() {
        lifecycleScope.launch {
            val schedule = memorizationViewModel.currentSchedule.value
            schedule?.let {
                memorizationViewModel.loadScheduleProgress()
            }
        }
    }

    private fun updateTodayProgress(target: DailyTarget) {
        val totalVerses = target.endVerse - target.startVerse + 1
        val completedText = if (target.isCompleted) {
            "$totalVerses/$totalVerses"
        } else {
            "${target.completedVerses}/$totalVerses"
        }
        binding.todayProgress.text = completedText

        when {
            target.isCompleted -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }

            target.completedVerses > 0 -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
            }

            else -> {
                binding.todayProgress.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScheduleCardWithTarget(target: DailyTarget) {
        val progressIndicator = when {
            target.isCompleted -> " ✓ (مكتمل)"
            target.completedVerses > 0 -> " (${target.completedVerses}/${target.getTotalVerses()} آية)"
            else -> ""
        }

        binding.scheduleTitle.text =
            "${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}$progressIndicator"
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                requireView().findNavController().popBackStack()
                true
            }

            else -> false
        }
    }

    private fun isFragmentInitializing(): Boolean {
        return !this::binding.isInitialized || binding.listOfRewat.adapter == null
    }

    private fun clearFormData() {
        if (!isFragmentInitializing()) {
            binding.listOfRewat.setText("")
            binding.listSouraName.setText("")
            binding.nbAya.setText("")
            binding.nbEyaEnd.setText("")
            binding.start.isEnabled = false
            reader = null
            soraId = 0
            startAya = 0
            endAya = 0
            selectedSurahName = ""

            enableAllFields(isNetworkAvailable())
        }
    }

    private fun restoreOfflineReciter() {
        val selectedReaderId = offlinePreferences.getString("selected_offline_reader_id", null)
        val selectedReaderName = offlinePreferences.getString("selected_offline_reader_name", null)

        if (selectedReaderId != null && selectedReaderName != null) {
            val offlineReciter = RecitersVerse(
                id = selectedReaderId,
                name = selectedReaderName,
                audio_url_bit_rate_128 = offlinePreferences.getString(
                    "selected_offline_reader_url_128",
                    ""
                ) ?: "",
                audio_url_bit_rate_64 = offlinePreferences.getString(
                    "selected_offline_reader_url_64",
                    ""
                ) ?: "",
                audio_url_bit_rate_32_ = offlinePreferences.getString(
                    "selected_offline_reader_url_32",
                    ""
                ) ?: "",
                musshaf_type = "",
                rewaya = ""
            )

            reader = offlineReciter
            readers.clear()
            readers.add(offlineReciter)

            val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, readers)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.listOfRewat.setAdapter(adapter)
            binding.listOfRewat.setText(offlineReciter.name, false)

            disableFieldsForOffline()
        }
    }

    override fun onResume() {
        super.onResume()
        val currentSchedule = memorizationViewModel.currentSchedule.value
        if (currentSchedule != null) {
            loadScheduleProgress()
            lifecycleScope.launch {
                val isCompleted = checkIfScheduleCompleted(currentSchedule)
                updateCreateScheduleButton(currentSchedule, isCompleted)
            }
        }
        clearFormData()
        if (!isNetworkAvailable()) {
            restoreOfflineReciter()
            if (isAdded && view != null) {
                val todayTarget = memorizationViewModel.todayTarget.value
                if (todayTarget != null) {
                    autoFillWithTodayTargetOffline(todayTarget)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadScheduleProgress()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkStateObserver = null
        job?.cancel()
    }
}