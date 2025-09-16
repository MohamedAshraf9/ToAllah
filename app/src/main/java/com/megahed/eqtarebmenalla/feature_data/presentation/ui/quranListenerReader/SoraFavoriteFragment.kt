package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.SoraFavoriteAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentSoraFavoriteBinding
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.toSong
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.myListener.OnItemReaderClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.navigation.findNavController
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.offline.OfflineUtils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SoraFavoriteFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentSoraFavoriteBinding
    private lateinit var soraFavoriteAdapter: SoraFavoriteAdapter
    private lateinit var mainViewModel: MainSongsViewModel
    private lateinit var quranListenerReaderViewModel: QuranListenerReaderViewModel

    @Inject
    lateinit var offlineAudioManager: OfflineAudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this).get(MainSongsViewModel::class.java)
        quranListenerReaderViewModel =
            ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSoraFavoriteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return root
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.title = getString(R.string.soraFav)
    }

    private fun setupRecyclerView() {
        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        soraFavoriteAdapter = SoraFavoriteAdapter(
            requireContext(),
            object : OnItemReaderClickListener<SoraSong> {
                override fun onItemClickReader(
                    itemObject: SoraSong,
                    view: View?,
                    readerName: String,
                ) {
                    lifecycleScope.launch {
                        try {
                            val isDownloaded = offlineAudioManager.isSurahDownloaded(
                                itemObject.readerId,
                                itemObject.SoraId
                            )
                            if (!isNetworkAvailable(requireContext()) && !isDownloaded) {
                                showOfflineAlert()
                                return@launch
                            } else {
                                val allFavoriteSongs = withContext(Dispatchers.IO) {
                                    getAllFavoriteSongsFromAllReaders()
                                }
                                FirebaseMusicSource._audiosLiveData.value = allFavoriteSongs

                                val songToPlay = allFavoriteSongs.find { song ->
                                    song.title == com.megahed.eqtarebmenalla.common.Constants.SORA_OF_QURAN[itemObject.SoraId] &&
                                            song.subtitle.contains(readerName.trim())
                                } ?: run {
                                    createSongForPlayback(itemObject, readerName.trim())
                                }

                                mainViewModel.playOrToggleSong(songToPlay, true)

                                val action: NavDirections =
                                    SoraFavoriteFragmentDirections.actionSoraFavoriteFragmentToSongFragment()
                                withContext(Dispatchers.Main) {
                                    requireView().findNavController().navigate(action)
                                }
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    binding.root,
                                    "حدث خطأ في تشغيل السورة",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }


                override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {}

                override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                    toggleFavoriteStatus(itemObject)
                }

                override fun onItemLongClick(itemObject: SoraSong, view: View?, position: Int) {}
            },
            offlineAudioManager,
            lifecycleScope
        )

        binding.recyclerView.adapter = soraFavoriteAdapter
    }

    private suspend fun getAllFavoriteSongsFromAllReaders(): List<Song> {
        return withContext(Dispatchers.IO) {
            val allFavorites =
                mutableListOf<Song>()

            val currentFavList = soraFavoriteAdapter.getAllFavSongs()

            val readerGroups = currentFavList.groupBy { it.readerId }

            readerGroups.forEach { (readerId, soraSongs) ->

                val reader = quranListenerReaderViewModel.getQuranListenerReaderById(readerId)
                val readerName = reader?.name ?: "قارئ"

                soraSongs.forEach { soraSong ->
                    val songEntity = createSongEntityWithCorrectUrl(soraSong, readerName)
                    allFavorites.add(songEntity)
                }
            }

            allFavorites
        }
    }

    private suspend fun createSongEntityWithCorrectUrl(
        soraSong: SoraSong,
        readerName: String,
    ): Song {
        return withContext(Dispatchers.IO) {
            val isDownloaded =
                offlineAudioManager.isSurahDownloaded(soraSong.readerId, soraSong.SoraId)

            val finalSoraSong = if (isDownloaded) {
                val offlineUrl =
                    offlineAudioManager.getOfflineAudioUrl(soraSong.readerId, soraSong.SoraId)
                if (offlineUrl != null) {
                    soraSong.copy(url = offlineUrl)
                } else {
                    soraSong
                }
            } else {
                soraSong
            }

            val song = finalSoraSong.toSong(readerName)

            val uniqueMediaId = if (isDownloaded) {
                "${soraSong.readerId}_${soraSong.SoraId}_offline"
            } else {
                "${soraSong.readerId}_${soraSong.SoraId}_online"
            }

            song.copy(
                mediaId = uniqueMediaId,
                songUrl = finalSoraSong.url
            )
        }
    }

    private suspend fun createSongForPlayback(
        soraSong: SoraSong,
        readerName: String,
    ): Song {
        return createSongEntityWithCorrectUrl(soraSong, readerName)
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            quranListenerReaderViewModel.getAllFavSorasOfReader().collect { readerList ->
                updateUI(readerList.isNotEmpty())
                soraFavoriteAdapter.updateData(readerList)
            }
        }

        lifecycleScope.launch {
            quranListenerReaderViewModel.downloadProgress.collect { progressMap ->

            }
        }
    }

    private fun updateUI(hasData: Boolean) {
        if (hasData) {
            binding.loadingContainer.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.lottieView.visibility = View.GONE
            binding.loadingText.visibility = View.GONE
        } else {
            binding.loadingContainer.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.lottieView.visibility = View.VISIBLE
            binding.loadingText.visibility = View.VISIBLE
        }
    }

    private fun toggleFavoriteStatus(soraSong: SoraSong) {
        soraSong.isVaForte = !soraSong.isVaForte
        quranListenerReaderViewModel.updateSoraSong(soraSong)

        val message = if (soraSong.isVaForte) {
            "تم إضافة السورة إلى المفضلة"
        } else {
            "تم إزالة السورة من المفضلة"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.favorites_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                requireView().findNavController().popBackStack()
            }

            R.id.action_download_all_favorites -> {
                showDownloadAllFavoritesDialog()
                true
            }

            R.id.action_clear_all_favorites -> {
                showClearAllFavoritesDialog()
                true
            }

            else -> false
        }
    }

    private fun showDownloadAllFavoritesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("تحميل جميع المفضلات")
            .setMessage("هل تريد تحميل جميع السور المفضلة؟")
            .setPositiveButton("تحميل") { _, _ ->
                downloadAllFavorites()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showClearAllFavoritesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف جميع المفضلات")
            .setMessage("هل تريد إزالة جميع السور من المفضلة؟")
            .setPositiveButton("حذف") { _, _ ->
                clearAllFavorites()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun downloadAllFavorites() {
        lifecycleScope.launch {
            try {
                var totalDownloaded = 0
                var totalFailed = 0

                quranListenerReaderViewModel.getAllFavSorasOfReader().collect { readerList ->
                    for (readerWithSora in readerList) {
                        val favSoras = readerWithSora.soraSongData.filter { it.isVaForte }

                        for (soraSong in favSoras) {
                            try {
                                val isAlreadyDownloaded = offlineAudioManager.isSurahDownloaded(
                                    readerWithSora.quranListenerReader.id,
                                    soraSong.SoraId
                                )

                                if (!isAlreadyDownloaded) {
                                    val success = quranListenerReaderViewModel.downloadAudio(
                                        readerId = readerWithSora.quranListenerReader.id,
                                        surahId = soraSong.SoraId,
                                        surahName = com.megahed.eqtarebmenalla.common.Constants.SORA_OF_QURAN[soraSong.SoraId],
                                        readerName = readerWithSora.quranListenerReader.name,
                                        audioUrl = soraSong.url
                                    )

                                    if (success) {
                                        totalDownloaded++
                                    } else {
                                        totalFailed++
                                    }
                                }
                            } catch (e: Exception) {
                                totalFailed++
                            }
                        }
                    }

                    val message = when {
                        totalDownloaded > 0 && totalFailed == 0 ->
                            "تم بدء تحميل $totalDownloaded سورة"

                        totalDownloaded > 0 && totalFailed > 0 ->
                            "تم بدء تحميل $totalDownloaded سورة، فشل في $totalFailed"

                        totalFailed > 0 ->
                            "فشل في تحميل $totalFailed سورة"

                        else ->
                            "جميع السور محملة مسبقاً"
                    }

                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    return@collect
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "حدث خطأ في تحميل المفضلات", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun showOfflineAlert() {
        if (!isAdded) return

        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("الاتصال بالإنترنت")
            setMessage("عذرًا، السورة غير متاحة للإستماع بدون اتصال بالإنترنت. يرجى الاتصال بالإنترنت أو اختيار سورة أخرى تم تحميلها مسبقاً.")
            setPositiveButton("إعدادات الاتصال") { _, _ ->
                openNetworkSettings()
            }
            setNegativeButton("البقاء دون اتصال") { dialog, _ ->
                dialog.dismiss()
            }
        }.show()
    }

    private fun openNetworkSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }

    private fun clearAllFavorites() {
        lifecycleScope.launch {
            try {
                quranListenerReaderViewModel.getAllFavSorasOfReader().collect { readerList ->
                    for (readerWithSora in readerList) {
                        val favSoras = readerWithSora.soraSongData.filter { it.isVaForte }

                        for (soraSong in favSoras) {
                            soraSong.isVaForte = false
                            quranListenerReaderViewModel.updateSoraSong(soraSong)
                        }
                    }

                    Snackbar.make(
                        binding.root,
                        "تم حذف جميع السور من المفضلة",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@collect
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "حدث خطأ في حذف المفضلات", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}