package com.megahed.eqtarebmenalla.feature_data.presentation.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.RequestManager
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.FragmentSongBinding
import com.megahed.eqtarebmenalla.exoplayer.isPlaying
import com.megahed.eqtarebmenalla.exoplayer.toSong
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.SongViewModel
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentSongBinding

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var offlineAudioManager: OfflineAudioManager

    private lateinit var mainViewModel: MainSongsViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var curPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null
    private var shouldUpdateSeekbar = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainSongsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = " "
        toolbar.setBackgroundColor(ContextCompat.getColor(App.getInstance(), R.color.transparent))

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupClickListeners()
        subscribeToObservers()

        return binding.root
    }

    private fun setupClickListeners() {
        binding.apply {
            ivPlayPauseDetail.setOnClickListener {
                curPlayingSong?.let { song ->
                    checkAndUpdateSongUrl(song) { updatedSong ->
                        mainViewModel.playOrToggleSong(updatedSong, true)
                    }
                }
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setCurPlayerTimeToTextView(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    shouldUpdateSeekbar = false
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        mainViewModel.seekTo(it.progress.toLong())
                        shouldUpdateSeekbar = true
                    }
                }
            })

            ivSkipPrevious.setOnClickListener {
                mainViewModel.skipToPreviousSong()
            }

            ivSkip.setOnClickListener {
                mainViewModel.skipToNextSong()
            }
        }
    }

    private fun checkAndUpdateSongUrl(song: Song, onUpdated: (Song) -> Unit) {
        lifecycleScope.launch {
            try {
                val readerId = extractReaderIdFromSong(song)
                val surahId = extractSurahIdFromSong(song)

                if (readerId != null && surahId != null) {
                    val offlineUrl = offlineAudioManager.getOfflineAudioUrl(readerId, surahId)

                    val updatedSong = if (offlineUrl != null && offlineUrl != song.mediaId) {
                        song.copy(mediaId = offlineUrl)
                    } else {
                        song
                    }

                    onUpdated(updatedSong)
                } else {
                    onUpdated(song)
                }
            } catch (e: Exception) {
                onUpdated(song)
            }
        }
    }

    private fun extractReaderIdFromSong(song: Song): String? {
        return try {
            song.subtitle?.let { subtitle ->
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractSurahIdFromSong(song: Song): Int? {
        return try {
            // If song ID represents surah number
            song.mediaId.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                result.data?.let { songs ->
                    if (curPlayingSong == null && songs.isNotEmpty()) {
                        curPlayingSong = songs[0]
                    }
                }
            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            curPlayingSong = it.toSong()
            updateTitleAndSongImage(curPlayingSong!!)
            updateOfflineStatus()
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            binding.ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.seekBar.progress = it?.position?.toInt() ?: 0
        }

        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if (shouldUpdateSeekbar) {
                binding.seekBar.progress = it.toInt()
                setCurPlayerTimeToTextView(it)
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            binding.seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            binding.tvSongDuration.text = dateFormat.format(it)
        }
    }

    private fun updateTitleAndSongImage(song: Song) {
        val title = "${song.title} - ${song.subtitle}"
        binding.tvSongName.text = title
    }

    private fun updateOfflineStatus() {
        lifecycleScope.launch {
            curPlayingSong?.let { song ->
                val readerId = extractReaderIdFromSong(song)
                val surahId = extractSurahIdFromSong(song)

                if (readerId != null && surahId != null) {
                    val isOffline = offlineAudioManager.isSurahDownloaded(readerId, surahId)

                    if (isOffline) {
                        binding.tvSongName.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_offline_24, 0, 0, 0
                        )
                    } else {
                        binding.tvSongName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                }
            }
        }
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.tvCurTime.text = dateFormat.format(ms)
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