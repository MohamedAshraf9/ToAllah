package com.megahed.eqtarebmenalla.feature_data.presentation.ui.schedule

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentScheduleCreationBinding
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MemorizationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class ScheduleCreationFragment : Fragment(), MenuProvider {

    private var _binding: FragmentScheduleCreationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MemorizationViewModel by viewModels()

    private var selectedSurahId: Int = -1
    private var selectedSurahName: String = ""
    private var selectedSurahVerseCount: Int = 0
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setupListeners()
        setupObservers()
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
        }
    }
    private fun setupDatePicker() {
        binding.etStartDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.etStartDate.setText(dateFormat.format(calendar.time))
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
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
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
            val daysNeeded = (totalVerses + dailyVerses - 1) / dailyVerses

            val completionCalendar = Calendar.getInstance().apply { time = calendar.time }
            completionCalendar.add(Calendar.DAY_OF_YEAR, daysNeeded)

            val completionDate = dateFormat.format(completionCalendar.time)
            binding.tvEstimatedCompletion.text = getString(R.string.estimated_completion_date, completionDate)

            binding.previewCard.visibility = View.VISIBLE
            binding.tvPreviewContent.text = getString(
                R.string.schedule_preview,
                binding.etScheduleTitle.text,
                selectedSurahName,
                startVerse,
                endVerse,
                dailyVerses,
                daysNeeded
            )
        } else {
            binding.tvEstimatedCompletion.text = getString(R.string.estimated_completion_undefined)
            binding.previewCard.visibility = View.GONE
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etScheduleTitle.text.isNullOrEmpty()) {
            binding.tilScheduleTitle.error = getString(R.string.error_schedule_title_required)
            return false
        }

        if (selectedSurahId == -1) {
            Snackbar.make(binding.root, getString(R.string.error_select_surah), Snackbar.LENGTH_SHORT).show()
            return false
        }

        val startVerse = binding.etStartVerse.text.toString().toIntOrNull() ?: 0
        val endVerse = binding.etEndVerse.text.toString().toIntOrNull() ?: 0

        if (startVerse < 1 || endVerse < 1 || endVerse < startVerse || endVerse > selectedSurahVerseCount) {
            Snackbar.make(binding.root, getString(R.string.error_invalid_verse_range), Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (binding.etStartDate.text.isNullOrEmpty()) {
            binding.tilStartDate.error = getString(R.string.error_start_date_required)
            return false
        }

        return true
    }

    private fun createSchedule() {
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
                    scheduleId = 0,
                    targetDate = normalizedDate,
                    surahId = selectedSurahId,
                    surahName = selectedSurahName,
                    startVerse = currentVerse,
                    endVerse = targetEndVerse,
                    estimatedDurationMinutes = 30
                )
            )

            currentVerse = targetEndVerse + 1
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        viewModel.createSchedule(title, description, startDate, endDate, dailyTargets)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

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