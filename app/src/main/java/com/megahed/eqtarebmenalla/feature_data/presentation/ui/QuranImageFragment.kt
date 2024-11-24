package com.megahed.eqtarebmenalla.feature_data.presentation.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranImageAdapter
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentQuranImageBinding
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.toSong
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource
import com.megahed.eqtarebmenalla.exoplayer.currentPlaybackPosition
import com.megahed.eqtarebmenalla.exoplayer.isPlaying
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImageItem
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener.QuranListenerViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader.QuranListenerReaderViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.QuranImageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@AndroidEntryPoint
class QuranImageFragment : Fragment(), MenuProvider {
    private lateinit var binding: FragmentQuranImageBinding

    private val viewModel: QuranImageViewModel by viewModels()
    private var soraId: Int? = null
    private var soraName: String? = null
    private lateinit var quranImageAdapter: QuranImageAdapter

    private lateinit var mainViewModel: MainSongsViewModel
    private lateinit var quranListenerReaderViewModel: QuranListenerReaderViewModel

    private var readerId: String? = "5"
    private var readerName: String? = "أحمد بن علي العجمي"
    private var currentSora: Song? = null

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var linearSnapHelper: LinearSnapHelper
    private var currentRecyclerViewPosition: Int = 0

    var soraNumbers = arrayListOf<Int>()
    var startAya:Int=0
    var endAya:Int=0
    private lateinit var sharedPreferences: SharedPreferences
    private var ayasList: List<QuranImageItem>? = null
    private var allSoraImages: List<QuranImageItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soraId = arguments?.let { QuranImageFragmentArgs.fromBundle(it).soraId }
        soraName = arguments?.let { QuranImageFragmentArgs.fromBundle(it).soraName }

        mainViewModel = ViewModelProvider(this).get(MainSongsViewModel::class.java)
        quranListenerReaderViewModel =
            ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)

        sharedPreferences = requireActivity().getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuranImageBinding.inflate(inflater, container, false)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = soraName ?: ""
        toolbar.setBackgroundColor(ContextCompat.getColor(App.getInstance(), R.color.transparent))

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val quranListenerViewModel =
            ViewModelProvider(this).get(QuranListenerViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.readerNameTextView.text = readerName
        lifecycleScope.launch {
            quranListenerReaderViewModel.getQuranListenerReaderById(readerId!!)
            getSoraAudio()
        }

        val editor = sharedPreferences.edit()
        editor.putBoolean("isPlayingSora", true)
        editor.apply()

        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        val readerIdFlow = savedStateHandle?.getLiveData<String>("readerId")?.asFlow()
        val readerNameFlow = savedStateHandle?.getLiveData<String>("readerName")?.asFlow()

        if (readerIdFlow != null && readerNameFlow != null) {
            lifecycleScope.launch {
                combine(readerIdFlow, readerNameFlow) { id, name ->
                    Pair(id, name)
                }
                    .distinctUntilChanged() // Emit only when the values actually change
                    .collect { (id, name) ->
                        if (readerId != id || readerName != name) {
                            readerId = id
                            readerName = name
                            binding.readerNameTextView.text = readerName
                            readerId?.let {
                                quranListenerReaderViewModel.getQuranListenerReaderById(
                                    readerId!!
                                )
                            }
                            getSoraAudio()
                        }
                }
            }
        }

        linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranImageAdapter = QuranImageAdapter(requireActivity())

        binding.recyclerView.adapter = quranImageAdapter

        linearSnapHelper = LinearSnapHelper()
        linearSnapHelper.attachToRecyclerView(binding.recyclerView)
        binding.recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    lifecycleScope.launch {
                        handleSnappedItem()
                        //getSoraImages()
                    }
                }
            }
        })

        lifecycleScope.launch {
        getSoraImages()
            }


        binding.playImageView.setOnClickListener {
            if (mainViewModel.isConnected.value?.peekContent()?.data == true) {
                mainViewModel.stopPlayback()
                lifecycleScope.launch {
                    readerId?.let {
                        val page = quranImageAdapter.getItemAt(currentRecyclerViewPosition)
                        ayasList = allSoraImages?.filter { it.page == page };
                        if (ayasList != null) {
                            val startTime = ayasList!![0].start_time
                            val endTime = ayasList!![ayasList!!.size - 1].end_time
                            if (currentSora != null) {
                                mainViewModel.playSoraSegment(
                                    currentSora!!,
                                    true,
                                    startTime!!,
                                    endTime!!
                                )
                            }
                        }

                    }
                }
            }
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            binding.playImageView.setImageResource(
                if(it?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        binding.playerCardView.setOnClickListener {
            mainViewModel.stopPlayback()
            val action: NavDirections =
                QuranImageFragmentDirections.actionQuranImageFragmentToReadersListFragment(soraId!!)
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.listeningOptionsImageView.setOnClickListener {
            showBottomSheet()
        }
    }

    private suspend fun getSoraImages() {
        soraId?.let {
            viewModel.getQuranImage(it, readerId?.toInt()!!)
            val state = viewModel.state.first { state ->
                !state.isLoading && state.quranImage.isNotEmpty()
            }
                        allSoraImages = state.quranImage
                    val hash = HashSet<String>()
                    state.quranImage.sortedBy { it.ayah }.forEach {
                        if (it.page != null) {
                            hash.add(it.page)
                        }
                    }
                    quranImageAdapter.setData(hash.sorted())
                    binding.toolbar.toolbar.title = currentSora?.title
                    handleSnappedItem()
                    if (state.isLoading) {
                        binding.progressBar.visibility = View.VISIBLE
                    } else {
                        binding.progressBar.visibility = View.GONE
                    }

                    val page = quranImageAdapter.getItemAt(currentRecyclerViewPosition)
                    ayasList = allSoraImages?.filter { it.page == page }
        }
    }

    private suspend fun getSoraAudio() {
        if (readerId == null) return
        readerId?.let {
            //quranListenerReaderViewModel.getQuranListenerReaderById(it)
            var songs: List<SoraSong>
            quranListenerReaderViewModel.getSongsOfSora(it)
                .distinctUntilChanged()
                .first {
                    FirebaseMusicSource._audiosLiveData.value =
                    it.map { it.toSong(readerName) }
                    songs = it
                    currentSora = songs.find { it.SoraId == soraId }?.toSong(readerName)
                    binding.toolbar.toolbar.title = currentSora?.title
                    it.isNotEmpty() }
        }
    }

    private fun handleSnappedItem() {

        // Find the snapped view
        val snappedView = linearSnapHelper.findSnapView(linearLayoutManager)

        if (snappedView != null) {
            val snappedPosition: Int = binding.recyclerView.getChildAdapterPosition(snappedView)

            if (snappedPosition != -1) {
                // Smoothly scroll to ensure proper snapping (force alignment)
                binding.recyclerView.smoothScrollToPosition(snappedPosition)
                currentRecyclerViewPosition = snappedPosition
            }else {
                currentRecyclerViewPosition = 0
            }

        } else {
            Log.d("RecyclerView", "No snapped view detected.")
        }
    }


    private fun showBottomSheet(){
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView: View = LayoutInflater.from(
            requireActivity()
        ).inflate(
            R.layout.bottom_sheet_listener_helper,
            requireView().findViewById<CoordinatorLayout>(R.id.container3)
        )

        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val soraList = bottomSheetView.findViewById<MaterialAutoCompleteTextView>(R.id.list_soura_name)
        val nbAya = bottomSheetView.findViewById<MaterialAutoCompleteTextView>(R.id.nb_aya)
        val nbAyaEnd = bottomSheetView.findViewById<MaterialAutoCompleteTextView>(R.id.nb_eya_end)
        val soraStartEndText = bottomSheetView.findViewById<TextInputLayout>(R.id.soraStartEndText)
        val soraStartEditText = bottomSheetView.findViewById<TextInputLayout>(R.id.soraStartEditText)
        val ayaRepeatEditText = bottomSheetView.findViewById<TextInputEditText>(R.id.aya_repeat_edit_text)
        val soraRepeatEditText = bottomSheetView.findViewById<TextInputEditText>(R.id.sura_repeat_edit_text)
        val startButton= bottomSheetView.findViewById<MaterialButton>(R.id.start)

        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        soraList.setAdapter(adapter)

        soraList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                nbAya.setText("")
                nbAyaEnd.setText("")
                startButton.isEnabled = false
                soraId = position + 1
                val sora = parent.getItemAtPosition(position) as String
                val soraNumber = Constants.SORA_OF_QURAN_WITH_NB_EYA[sora]!!
                soraNumbers.clear()
                for (i in 1..soraNumber) {
                    soraNumbers.add(i)
                }

                val adapter1 =
                    ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                nbAya.setAdapter(adapter1)

                val adapter2 =
                    ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
                adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                nbAyaEnd.setAdapter(adapter2)

                soraStartEditText.isEnabled = true
            }

        nbAya.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, position, id ->
                startAya = adapterView.getItemAtPosition(position) as Int
                soraStartEndText.isEnabled = true
            }

        nbAyaEnd.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, position, id ->
                endAya = adapterView.getItemAtPosition(position) as Int
                startButton.isEnabled = true
            }

        startButton.setOnClickListener {

            lifecycleScope.launch {
                getSoraImages()
                getSoraAudio()
                startPlaying(ayasList?.subList(startAya - 1, endAya)!!, ayaRepeatEditText?.text.toString().trim(), soraRepeatEditText?.text.toString().trim())
            }
            bottomSheetDialog.dismiss()

        }


        //code
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.dismissWithAnimation = true
        bottomSheetDialog.window?.attributes?.windowAnimations =
            R.style.DialogAnimation

        bottomSheetDialog.show()
    }

    private fun startPlaying(ayasList: List<QuranImageItem>, ayaRepeat: String, soraRepeat: String) {
        if (MethodHelper.isOnline(requireContext())) {
            if (ayaRepeat.trim().isEmpty() || soraRepeat.trim().isEmpty()) {
                MethodHelper.toastMessage(getString(R.string.addValidData))
            } else {
                if (startAya > endAya) {
                    MethodHelper.toastMessage(getString(R.string.ayaWrong))
                } else {
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isPlayingSora", true)
                    editor.apply()

                    lifecycleScope.launch {
                        readerId?.let {
                            if (!ayasList.isNullOrEmpty() && currentSora != null) {
                                for (k in 0 until soraRepeat.toInt()) { // Repeat entire sora
                                    for (aya in ayasList) { // Play each aya
                                        val startTime = aya.start_time
                                        val endTime = aya.end_time

                                        if (startTime != null && endTime != null) {
                                            for (i in 1..ayaRepeat.toInt()) { // Repeat current aya

                                                mainViewModel.playSoraSegment(
                                                    currentSora!!,
                                                    false,
                                                    startTime = startTime,
                                                    endTime = endTime
                                                )

                                                // Wait for the duration of the aya to complete playback
                                                monitorAyaPlayback(endTime.toLong())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            MethodHelper.toastMessage(getString(R.string.checkConnection))
        }
    }


    private suspend fun monitorAyaPlayback(endTime: Long) {
        while (true) {
            val playbackPosition = mainViewModel.playbackState.value?.currentPlaybackPosition
            if (playbackPosition != null && playbackPosition >= endTime) {
                break
            }
            delay(100) // Check every 100ms
        }

        // Add a short buffer before moving to the next segment
        delay(300)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
            }

            else -> false
        }
    }


}