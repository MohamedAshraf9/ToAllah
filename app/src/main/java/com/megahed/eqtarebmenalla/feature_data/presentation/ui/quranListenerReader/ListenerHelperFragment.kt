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
import android.util.Log
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
import com.megahed.eqtarebmenalla.db.model.OfflineSettings
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.HefzViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import com.megahed.eqtarebmenalla.offline.NetworkStateObserver
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import jakarta.inject.Inject

@AndroidEntryPoint
class ListenerHelperFragment : Fragment(), MenuProvider {

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
        sharedPreferences = requireActivity().getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
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
        binding.listOfRewat.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            binding.listSouraName.setText("")
            binding.nbAya.setText("")
            binding.nbEyaEnd.setText("")
            binding.start.isEnabled = false
            reader = parent.getItemAtPosition(position) as RecitersVerse
            binding.soraStartSpinner.isEnabled = true
        }

        binding.listSouraName.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            binding.nbAya.setText("")
            binding.nbEyaEnd.setText("")
            binding.start.isEnabled = false

            val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
            val selectedSurahName = surahNames[position]
            soraId = position + 1

            setupVerseSelection(selectedSurahName)
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

        binding.start.setOnClickListener {
            handleStartMemorization()
        }

        binding.memorizationScheduleCard.setOnClickListener {
            requireView().findNavController()
                .navigate(R.id.action_listenerHelperFragment_to_scheduleCreationFragment)
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
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                selectedReader.audio_url_bit_rate_128.trim().isNotEmpty() && selectedReader.audio_url_bit_rate_128.trim() != "0" ->
                    selectedReader.audio_url_bit_rate_128
                selectedReader.audio_url_bit_rate_64.trim().isNotEmpty() && selectedReader.audio_url_bit_rate_64.trim() != "0" ->
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
                val filter = ayaHefzState.recitersVerse.filter {
                    (it.audio_url_bit_rate_32_.trim().isNotEmpty() && it.audio_url_bit_rate_32_.trim() != "0") ||
                            (it.audio_url_bit_rate_64.trim().isNotEmpty() && it.audio_url_bit_rate_64.trim() != "0") ||
                            (it.audio_url_bit_rate_128.trim().isNotEmpty() && it.audio_url_bit_rate_128.trim() != "0")
                }
                readers.clear()
                readers.addAll(filter)

                if (readers.isNotEmpty()) {
                    val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, readers)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.listOfRewat.setAdapter(adapter)
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
                } else {
                    binding.memorizationScheduleCard.visibility = View.GONE
                    binding.btnMemorizationTracker.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            memorizationViewModel.todayTarget.collectLatest { target ->
                if (target != null) {
                    updateTodayProgress(target)
                    updateScheduleCardWithTarget(target)
                } else {
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
                    val verseDetails = "${verseProgress.completedVerses}/${verseProgress.totalVerses}"
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
            }
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
                readerId = selectedReader.id.toString(),
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
                        Snackbar.make(binding.root, "لا يمكن فتح الإعدادات", Snackbar.LENGTH_SHORT).show()
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
        val completedText = if (target.isCompleted) "$totalVerses/$totalVerses" else "0/$totalVerses"
        binding.todayProgress.text = completedText

        if (target.isCompleted) {
            binding.todayProgress.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.green)
            )
        } else {
            binding.todayProgress.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScheduleCardWithTarget(target: DailyTarget) {
        val statusText = if (target.isCompleted) " (مكتمل)" else ""
        binding.scheduleTitle.text = "${target.surahName} - الآيات ${target.startVerse}-${target.endVerse}$statusText"
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

            val verseAdapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
            verseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.nbAya.setAdapter(verseAdapter)
            binding.nbEyaEnd.setAdapter(verseAdapter)

            binding.nbAya.setText(target.startVerse.toString(), false)
            binding.nbEyaEnd.setText(target.endVerse.toString(), false)

            startAya = target.startVerse
            endAya = target.endVerse

            binding.soraStartSpinner.isEnabled = true
            binding.soraStartEditText.isEnabled = true
            binding.soraStartEndText.isEnabled = true
            binding.start.isEnabled = reader != null
        }
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