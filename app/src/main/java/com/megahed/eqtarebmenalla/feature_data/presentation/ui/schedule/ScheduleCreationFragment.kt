package com.megahed.eqtarebmenalla.feature_data.presentation.ui.schedule

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentScheduleCreationBinding
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.RecitersVerse
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.HefzViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.toString

@AndroidEntryPoint
class ScheduleCreationFragment : Fragment(), MenuProvider {

    private var _binding: FragmentScheduleCreationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemorizationViewModel by viewModels()
    private var existingSchedule: MemorizationSchedule? = null
    private var scheduleId: Long = -1L
    private lateinit var sharedPreferences: SharedPreferences
    private val hefzViewModel: HefzViewModel by activityViewModels()

    private var selectedSurahId: Int = -1
    private var selectedSurahName: String = ""
    private var selectedSurahVerseCount: Int = 0
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var selectedOfflineReader: RecitersVerse? = null
    private var availableReaders = mutableListOf<RecitersVerse>()
    private val memorizationViewModel: MemorizationViewModel by viewModels()

    @Inject
    lateinit var offlineAudioManager: OfflineAudioManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScheduleCreationBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSurahSpinner()
        setupDatePicker()
        setupOfflineControls()
        setupListeners()
        setupObservers()
        loadMemorizationReaders()
        setupButtonForCreateMode()
        sharedPreferences = requireActivity().getSharedPreferences("offline_prefs", Context.MODE_PRIVATE)
        scheduleId = arguments?.getLong("scheduleId", -1L) ?: -1L

        if (scheduleId != -1L) {
            loadExistingSchedule()
        }
    }

    private fun loadExistingSchedule() {
        lifecycleScope.launch {
            try {
                val (schedule, targets) = memorizationViewModel.getScheduleWithTargets(scheduleId)

                schedule?.let {
                    existingSchedule = it
                    populateFormWithScheduleData(it, targets)
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "خطأ في تحميل بيانات الجدول: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createSchedule() {
        try {
            val title = binding.etScheduleTitle.text.toString()
            val description = binding.etScheduleDescription.text.toString()

            val startCalendar = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDate = startCalendar.time

            val startVerse = binding.etStartVerse.text.toString().toInt()
            val endVerse = binding.etEndVerse.text.toString().toInt()
            val dailyVerses = binding.etDailyVerses.text.toString().toInt()

            val totalVerses = endVerse - startVerse + 1
            val daysNeeded = (totalVerses + dailyVerses - 1) / dailyVerses
            val endCalendar = Calendar.getInstance().apply {
                time = startDate
                add(Calendar.DAY_OF_YEAR, daysNeeded)
            }
            val endDate = endCalendar.time

            val dailyTargets = mutableListOf<DailyTarget>()
            var currentVerse = startVerse
            val currentDate = Calendar.getInstance().apply { time = startDate }

            while (currentVerse <= endVerse) {
                val targetEndVerse = minOf(currentVerse + dailyVerses - 1, endVerse)

                val normalizedDate = Calendar.getInstance().apply {
                    time = currentDate.time
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                dailyTargets.add(
                    DailyTarget(
                        scheduleId = existingSchedule?.id ?: 0,
                        targetDate = normalizedDate,
                        surahId = selectedSurahId,
                        surahName = selectedSurahName,
                        startVerse = currentVerse,
                        endVerse = targetEndVerse,
                        estimatedDurationMinutes = (targetEndVerse - currentVerse + 1) * 5
                    )
                )

                currentVerse = targetEndVerse + 1
                currentDate.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (existingSchedule != null) {
                // Update existing schedule
                val updatedSchedule = existingSchedule!!.copy(
                    title = title,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    updatedAt = Date()
                )
                viewModel.updateScheduleWithTargets(updatedSchedule, dailyTargets)
            } else {
                viewModel.createSchedule(title, description, startDate, endDate, dailyTargets)
            }

        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "خطأ في ${if (existingSchedule != null) "تحديث" else "إنشاء"} الجدول: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    fun Date.formatToUi(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(this)
    }

    private fun setupButtonForCreateMode() {
        binding.btnCreateSchedule.text = "إنشاء الجدول"
        binding.btnCreateSchedule.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_add, // Left drawable (add/create icon)
            0, 0, 0
        )
        binding.toolbar.toolbar.title = getString(R.string.create_memorization_schedule)
    }

    private fun setupButtonForEditMode() {
        binding.btnCreateSchedule.text = "تحديث"
        binding.toolbar.toolbar.title = "تعديل جدول الحفظ"
        binding.stepTitle.text = "تعديل الجدول الحالي"
    }

    private fun populateFormWithScheduleData(
        schedule: MemorizationSchedule,
        targets: List<DailyTarget>,
    ) {
        binding.etScheduleTitle.setText(schedule.title)
        binding.etScheduleDescription.setText(schedule.description ?: "")
        binding.etStartDate.setText(schedule.startDate.formatToUi())

        setupButtonForEditMode()

        if (targets.isNotEmpty()) {
            val firstTarget = targets.first()
            val lastTarget = targets.last()

            selectedSurahId = firstTarget.surahId
            selectedSurahName = firstTarget.surahName

            val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }
            val surahIndex = surahNames.indexOf(selectedSurahName)

            if (surahIndex >= 0) {
                binding.spinnerSurah.setText(selectedSurahName, false)
                selectedSurahVerseCount =
                    Constants.SORA_OF_QURAN_WITH_NB_EYA[selectedSurahName] ?: 0

                binding.etStartVerse.setText(firstTarget.startVerse.toString())
                binding.etEndVerse.setText(lastTarget.endVerse.toString())

                val avgDailyVerses = if (targets.size > 1) {
                    val totalVerses = lastTarget.endVerse - firstTarget.startVerse + 1
                    totalVerses / targets.size
                } else {
                    firstTarget.endVerse - firstTarget.startVerse + 1
                }
                binding.etDailyVerses.setText(avgDailyVerses.toString())

                updateVerseCount()
                calculateEstimatedCompletion()
            }
        }

        calendar.time = schedule.startDate
    }

    private fun loadMemorizationReaders() {
        lifecycleScope.launch {
            hefzViewModel.state.collect { ayaHefzState ->
                val filteredReaders = ayaHefzState.recitersVerse.filter {
                    (it.audio_url_bit_rate_32_.trim()
                        .isNotEmpty() && it.audio_url_bit_rate_32_.trim() != "0") ||
                            (it.audio_url_bit_rate_64.trim()
                                .isNotEmpty() && it.audio_url_bit_rate_64.trim() != "0") ||
                            (it.audio_url_bit_rate_128.trim()
                                .isNotEmpty() && it.audio_url_bit_rate_128.trim() != "0")
                }

                availableReaders.clear()
                availableReaders.addAll(filteredReaders)

                setupOfflineReaderSpinner()
            }
        }
    }
    private fun downloadScheduleForOffline() {
        selectedOfflineReader?.let { reader ->
            lifecycleScope.launch {
                try {
                    showDownloadProgress(0, 100)

                    val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 1
                    val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 1
                    val totalVerses = endVerse - startVerse + 1

                    Log.d(
                        "SCHEDULE_DOWNLOAD",
                        "Downloading verses $startVerse to $endVerse (total: $totalVerses)"
                    )

                    val baseUrl = reader.audio_url_bit_rate_128.trim().ifEmpty {
                        reader.audio_url_bit_rate_64.trim().ifEmpty {
                            reader.audio_url_bit_rate_32_.trim()
                        }
                    }.trimEnd('/')

                    var downloadedCount = 0
                    val downloadTasks = mutableListOf<Deferred<Boolean>>()

                    for (verseNumber in startVerse..endVerse) {
                        val task = async {
                            delay((verseNumber - startVerse) * 200L)

                            val surahFormatted = String.format(Locale.US, "%03d", selectedSurahId)
                            val verseFormatted = String.format(Locale.US, "%03d", verseNumber)
                            val verseUrl = "$baseUrl/${surahFormatted}${verseFormatted}.mp3"
                            val verseName = "${selectedSurahName}_آية_${verseNumber}"

                            val success = offlineAudioManager.downloadVerseAudio(
                                readerId = normalizeToAsciiDigits(reader.id.toString()),
                                surahId = selectedSurahId,
                                verseId = verseNumber,
                                verseName = verseName,
                                readerName = reader.name,
                                audioUrl = verseUrl
                            )

                            if (success) {
                                withContext(Dispatchers.Main) {
                                    downloadedCount++
                                    val progress = (downloadedCount * 100) / totalVerses
                                    showDownloadProgress(progress, 100)
                                }
                            }

                            success
                        }
                        downloadTasks.add(task)
                    }

                    val results = downloadTasks.awaitAll()
                    val successCount = results.count { it }

                    hideDownloadProgress()
                    if (successCount == totalVerses) {
                        Snackbar.make(
                            binding.root,
                            "تم تحميل جميع الآيات بنجاح ($successCount آية)",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "تم تحميل $successCount من $totalVerses آية",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    hideDownloadProgress()
                    Snackbar.make(
                        binding.root,
                        "خطأ في تحميل الآيات: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun downloadFullScheduleRange() {
        selectedOfflineReader?.let { reader ->
            lifecycleScope.launch {
                try {
                    val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 1
                    val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 1
                    val totalVerses = endVerse - startVerse + 1

                    val baseUrl = reader.audio_url_bit_rate_128.trim().ifEmpty {
                        reader.audio_url_bit_rate_64.trim().ifEmpty {
                            reader.audio_url_bit_rate_32_.trim()
                        }
                    }.trimEnd('/')

                    var downloadedCount = 0

                    for (verseNumber in startVerse..endVerse) {
                        val surahFormatted = String.format(Locale.US, "%03d", selectedSurahId)
                        val verseFormatted = String.format(Locale.US, "%03d", verseNumber)
                        val verseUrl = "$baseUrl/${surahFormatted}${verseFormatted}.mp3"
                        val verseName = "${selectedSurahName}_آية_${verseNumber}"

                        Log.d("DEBUG_DOWNLOAD", "Downloading verse $verseNumber: $verseUrl")

                        val success = offlineAudioManager.downloadAudio(
                            readerId = normalizeToAsciiDigits(reader.id.toString()),
                            surahId = selectedSurahId,
                            surahName = verseName,
                            readerName = reader.name,
                            audioUrl = verseUrl
                        )

                        if (success) {
                            downloadedCount++
                            val progress = (downloadedCount * 100) / totalVerses
                            showDownloadProgress(progress, 100)
                            delay(500)
                        }
                    }

                    hideDownloadProgress()
                    if (downloadedCount == totalVerses) {
                        Snackbar.make(
                            binding.root,
                            "تم تحميل جميع الآيات بنجاح ($downloadedCount آية)",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            "تم تحميل $downloadedCount من $totalVerses آية",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    hideDownloadProgress()
                    Snackbar.make(
                        binding.root,
                        "خطأ في تحميل الآيات: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun normalizeToAsciiDigits(input: String): String {
        return input.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }

    private fun updateOfflineSettings() {
        lifecycleScope.launch {
            selectedOfflineReader?.let { reader ->
                val settings = offlineAudioManager.getOfflineSettings()
                val updatedSettings = settings.copy(
                    selectedOfflineReaderId = normalizeToAsciiDigits(reader.id),
                    selectedOfflineReaderName = reader.name
                )
                offlineAudioManager.updateOfflineSettings(updatedSettings)

                sharedPreferences.edit {
                    putString("selected_offline_reader_id", normalizeToAsciiDigits(reader.id))
                    putString("selected_offline_reader_name", reader.name)
                    putString("selected_offline_reader_url_128", reader.audio_url_bit_rate_128)
                    putString("selected_offline_reader_url_64", reader.audio_url_bit_rate_64)
                    putString("selected_offline_reader_url_32", reader.audio_url_bit_rate_32_)
                }
            }
        }
    }

    private fun setupOfflineReaderSpinner() {
        if (availableReaders.isNotEmpty()) {
            val adapter =
                ArrayAdapter(requireContext(), R.layout.list_item_spinner, availableReaders)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerOfflineReader.setAdapter(adapter)

            binding.spinnerOfflineReader.setOnItemClickListener { _, _, position, _ ->
                selectedOfflineReader = availableReaders[position]
                updateOfflineSettings()
                if (binding.switchOfflineMemorization.isChecked) {
                    binding.btnDownloadSchedule.visibility = View.VISIBLE
                }
            }
        }
    }


    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = getString(R.string.create_memorization_schedule)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupOfflineControls() {
        binding.switchOfflineMemorization.setOnCheckedChangeListener { _, isChecked ->
            binding.tilOfflineReader.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.btnDownloadSchedule.visibility =
                if (isChecked && selectedOfflineReader != null) View.VISIBLE else View.GONE

            lifecycleScope.launch {
                val settings = offlineAudioManager.getOfflineSettings()
                val updatedSettings = settings.copy(isOfflineMemorizationEnabled = isChecked)
                offlineAudioManager.updateOfflineSettings(updatedSettings)
            }

            if (!isChecked) {
                hideDownloadProgress()
                selectedOfflineReader = null
            }
        }

        binding.btnDownloadSchedule.setOnClickListener {
            if (validateInputsForDownload()) {
                downloadScheduleForOffline()
            }
        }
    }

    private fun validateInputsForDownload(): Boolean {
        if (selectedSurahId == -1) {
            Snackbar.make(binding.root, "يرجى اختيار السورة أولاً", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (selectedOfflineReader == null) {
            Snackbar.make(
                binding.root,
                "يرجى اختيار القارئ للوضع غير المتصل",
                Snackbar.LENGTH_SHORT
            ).show()
            return false
        }

        val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 0
        val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 0

        if (startVerse < 1 || endVerse < 1 || endVerse < startVerse) {
            Snackbar.make(binding.root, "يرجى إدخال نطاق صحيح للآيات", Snackbar.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun getReaderAudioUrl(reader: RecitersVerse): String {
        val rawBase = when {
            reader.audio_url_bit_rate_32_.trim()
                .isNotEmpty() && reader.audio_url_bit_rate_32_.trim() != "0" ->
                reader.audio_url_bit_rate_32_

            reader.audio_url_bit_rate_64.trim()
                .isNotEmpty() && reader.audio_url_bit_rate_64.trim() != "0" ->
                reader.audio_url_bit_rate_64

            else -> reader.audio_url_bit_rate_128
        }.trim()

        val base = rawBase
            .replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
            .removeSuffix("/")

        val three = String.format("%03d", selectedSurahId)
        return "$base/$three.mp3"
    }

    private fun monitorDownloadProgress(readerId: String, surahId: Int) {
        lifecycleScope.launch {
            offlineAudioManager.downloadProgress.collect { progressMap ->
                val downloadId = "${readerId}_${surahId}"
                val progress = progressMap[downloadId]

                if (progress != null) {
                    showDownloadProgress(progress, 100)
                } else {
                    val isDownloaded = offlineAudioManager.isAudioDownloaded(readerId, surahId)
                    if (isDownloaded) {
                        hideDownloadProgress()
                        Snackbar.make(binding.root, "تم التحميل بنجاح", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun setupSurahSpinner() {
        val surahNames = Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key }

        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, surahNames)
        binding.spinnerSurah.setAdapter(adapter)

        binding.spinnerSurah.setOnItemClickListener { _, _, position, _ ->
            selectedSurahName = surahNames[position]
            selectedSurahId = position + 1

            selectedSurahVerseCount = Constants.SORA_OF_QURAN_WITH_NB_EYA[selectedSurahName] ?: 0

            binding.etStartVerse.setText("1")
            binding.etEndVerse.setText(selectedSurahVerseCount.toString())
            updateVerseCount()
            calculateEstimatedCompletion()

            if (binding.switchOfflineMemorization.isChecked && selectedOfflineReader != null) {
                binding.btnDownloadSchedule.visibility = View.VISIBLE
            }
        }
    }

    private fun setupDatePicker() {
        binding.etStartDate.isFocusable = false
        binding.etStartDate.isClickable = true
        binding.etStartDate.keyListener = null

        binding.etStartDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.etStartDate.setText(dateFormat.format(calendar.time))
                    binding.tilStartDate.error = null
                    calculateEstimatedCompletion()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }
    }

    private fun setupListeners() {
        binding.etStartVerse.addTextChangedListener {
            updateVerseCount()
            calculateEstimatedCompletion()
        }

        binding.etEndVerse.addTextChangedListener {
            updateVerseCount()
            calculateEstimatedCompletion()
        }

        binding.etDailyVerses.addTextChangedListener {
            calculateEstimatedCompletion()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCreateSchedule.setOnClickListener {
            if (validateInputs()) {
                createSchedule()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.btnCreateSchedule.isEnabled = !state.isLoading

                state.message?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                    viewModel.dismissMessage()
                    if (state.lastCreatedScheduleId != null) {
                        findNavController().popBackStack()
                    }
                }

                state.error?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.dismissError()
                }
            }
        }
    }

    private fun showDownloadProgress(current: Int, total: Int) {
        binding.downloadProgressLayout.visibility = View.VISIBLE
        binding.btnDownloadSchedule.isEnabled = false

        if (total > 0) {
            binding.progressDownload.isIndeterminate = false
            binding.progressDownload.max = total
            binding.progressDownload.progress = current
            binding.tvDownloadStatus.text = "جاري التحميل: $current%"
        } else {
            binding.progressDownload.isIndeterminate = true
            binding.tvDownloadStatus.text = "جاري تحضير التحميل..."
        }
    }

    private fun hideDownloadProgress() {
        binding.downloadProgressLayout.visibility = View.GONE
        binding.btnDownloadSchedule.isEnabled = true
    }

    private fun updateVerseCount() {
        val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 0
        val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 0

        if (startVerse > 0 && endVerse > 0 && endVerse >= startVerse) {
            val verseCount = endVerse - startVerse + 1
            binding.tvVerseCount.text = getString(R.string.total_verses, verseCount)
        } else {
            binding.tvVerseCount.text = getString(R.string.total_verses_undefined)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateEstimatedCompletion() {
        val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 0
        val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 0
        val dailyVerses = binding.etDailyVerses.text.toString().toIntOrNull() ?: 5

        if (startVerse > 0 && endVerse > 0 && endVerse >= startVerse && dailyVerses > 0) {
            val totalVerses = endVerse - startVerse + 1
            val daysNeeded = if (totalVerses <= dailyVerses) {
                0
            } else {
                (totalVerses - dailyVerses + dailyVerses - 1) / dailyVerses
            }

            val completionCalendar = Calendar.getInstance().apply { time = calendar.time }
            if (daysNeeded > 0) {
                completionCalendar.add(Calendar.DAY_OF_YEAR, daysNeeded)
            }

            val completionDate = dateFormat.format(completionCalendar.time)
            binding.tvEstimatedCompletion.text =
                getString(R.string.estimated_completion_date, completionDate)

            binding.previewCard.visibility = View.VISIBLE
            val actualDays = daysNeeded + 1
            binding.tvPreviewContent.text = getString(
                R.string.schedule_preview,
                binding.etScheduleTitle.text,
                selectedSurahName,
                startVerse,
                endVerse,
                dailyVerses,
                actualDays
            )
        } else {
            binding.tvEstimatedCompletion.text = getString(R.string.estimated_completion_undefined)
            binding.previewCard.visibility = View.GONE
        }
    }
    private fun validateInputs(): Boolean {
        var isValid = true

        binding.tilScheduleTitle.error = null
        binding.tilStartDate.error = null
        binding.tilDailyVerses.error = null

        if (binding.etScheduleTitle.text.isNullOrEmpty()) {
            binding.tilScheduleTitle.error = getString(R.string.error_schedule_title_required)
            isValid = false
        }

        if (selectedSurahId == -1) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_select_surah),
                Snackbar.LENGTH_SHORT
            ).show()
            isValid = false
        }

        val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 0
        val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 0
        val dailyVerses = binding.etDailyVerses.text.toString().toIntOrNull() ?: 0

        if (startVerse < 1 || endVerse < 1 || endVerse < startVerse || endVerse > selectedSurahVerseCount) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_invalid_verse_range),
                Snackbar.LENGTH_SHORT
            ).show()
            isValid = false
        }
        val maxAllowed = if (endVerse >= startVerse) endVerse - startVerse + 1 else 0
        if (dailyVerses > maxAllowed) {
            binding.tilDailyVerses.error = "أقصى عدد الآيات اليومية $maxAllowed"
            isValid = false
        }

        if (binding.etStartDate.text.isNullOrEmpty()) {
            binding.tilStartDate.error = getString(R.string.error_start_date_required)
            isValid = false
        }

        if (binding.switchOfflineMemorization.isChecked && selectedOfflineReader == null) {
            Snackbar.make(binding.root, "يرجى اختيار قارئ للوضع غير المتصل", Snackbar.LENGTH_SHORT)
                .show()
            isValid = false
        }

        return isValid
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }

            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class TempSchedule(
    val surahId: Int,
    val surahName: String,
    val startVerse: Int,
    val endVerse: Int,
)