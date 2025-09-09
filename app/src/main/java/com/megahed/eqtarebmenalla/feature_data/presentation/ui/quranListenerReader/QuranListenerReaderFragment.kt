package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerReaderAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.common.Constants.SORA_OF_QURAN
import com.megahed.eqtarebmenalla.common.Constants.getSoraLink
import com.megahed.eqtarebmenalla.common.Constants.songs
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerReaderBinding
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.toSong
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener.QuranListenerFragmentDirections
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import com.megahed.eqtarebmenalla.offline.OfflineUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class QuranListenerReaderFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    private lateinit var quranListenerReaderAdapter: QuranListenerReaderAdapter
    private lateinit var mainViewModel: MainSongsViewModel
    private lateinit var quranListenerReaderViewModel: QuranListenerReaderViewModel

    private var readerId: String? = null
    private var readerName: String? = null
    private var quranListenerReader: QuranListenerReader? = null

    private val quranReaderSoras: MutableList<SoraSong> = Collections.synchronizedList(mutableListOf<SoraSong>())
    private lateinit var offlineAudioManager: OfflineAudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readerId = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).id }
        readerName = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).readerName }

        mainViewModel = ViewModelProvider(this).get(MainSongsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        quranListenerReaderViewModel = ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)
        offlineAudioManager = quranListenerReaderViewModel.getOfflineAudioManager()

        val toolbar: Toolbar = binding.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return root
    }

    private fun setupUI() {
        lifecycleScope.launchWhenStarted {
            readerId?.let {
                quranListenerReaderViewModel.getQuranListenerReaderById(it)?.let {
                    binding.readerName.text = it.name
                    binding.rewaya.text = it.rewaya
                    binding.soraNumbers.text = it.count
                    binding.readerChar.text = it.letter
                    isFav(it.isVaForte)
                    quranListenerReader = it
                }
            }
        }

        var isShow = true
        var scrollRange = -1
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0) {
                binding.toolbarLayout.title = readerName
                binding.toolbar.title = readerName
                isShow = true
            } else if (isShow) {
                binding.toolbarLayout.title = " "
                binding.toolbar.title = " "
                isShow = false
            }
        })
    }

    private fun setupRecyclerView() {
        val verticalLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerReaderAdapter = QuranListenerReaderAdapter(
            requireContext(),
            object : OnItemWithFavClickListener<SoraSong> {

                override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launchNotificationPermission()
                    }

                    val song = itemObject.toSong(readerName)
                    mainViewModel.playOrToggleSong(song, true)

                    val action: NavDirections =
                        QuranListenerReaderFragmentDirections.actionQuranListenerReaderFragmentToSongFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                }

                override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                    itemObject.isVaForte = !itemObject.isVaForte
                    quranListenerReaderViewModel.updateSoraSong(itemObject)
                }

                override fun onItemLongClick(itemObject: SoraSong, view: View?, position: Int) {
                    showDownloadDialog(itemObject)
                }
            },
            offlineAudioManager,
            readerId ?: "",
            lifecycleScope
        )
        binding.recyclerView.adapter = quranListenerReaderAdapter
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            readerId?.let {
                quranListenerReaderViewModel.getSongsOfSora(it).collect { soraSongs ->
                    FirebaseMusicSource._audiosLiveData.value = soraSongs.map { it.toSong(readerName) }
                    quranListenerReaderAdapter.setData(soraSongs)
                    quranReaderSoras.clear()
                    quranReaderSoras.addAll(soraSongs)

                    updateAdapterWithDownloadStatus()
                }
            }
        }

        lifecycleScope.launch {
            quranListenerReaderViewModel.downloadProgress.collect { progressMap ->
                quranListenerReaderAdapter.updateDownloadProgress(progressMap)
            }
        }
    }

    private fun setupClickListeners() {
        binding.favorite.setOnClickListener {
            quranListenerReader?.let {
                it.isVaForte = !it.isVaForte
                quranListenerReaderViewModel.updateQuranListenerReader(it)
                isFav(it.isVaForte)
            }
        }

        binding.download.setOnClickListener {
            showBulkDownloadDialog()
        }
    }
    private suspend fun updateAdapterWithDownloadStatus() {
        readerId?.let { readerIdValue ->
            withContext(Dispatchers.IO) {
                try {
                    val downloadStatusMap = mutableMapOf<Int, Boolean>()
                    val sorasCopy = quranReaderSoras.toList()

                    sorasCopy.forEach { soraSong ->
                        val isDownloaded = quranListenerReaderViewModel.isAudioDownloaded(readerIdValue, soraSong.SoraId)
                        downloadStatusMap[soraSong.SoraId] = isDownloaded
                    }

                    withContext(Dispatchers.Main) {
                        quranListenerReaderAdapter.updateDownloadStatus(downloadStatusMap)
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun showBulkDownloadDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val downloadedCount = readerId?.let { readerIdValue ->
                    val sorasCopy = synchronized(quranReaderSoras) {
                        quranReaderSoras.toList()
                    }

                    sorasCopy.count { soraSong ->
                        quranListenerReaderViewModel.isAudioDownloaded(readerIdValue, soraSong.SoraId)
                    }
                } ?: 0

                val totalCount = synchronized(quranReaderSoras) {
                    quranReaderSoras.size
                }

                withContext(Dispatchers.Main) {
                    if (downloadedCount == totalCount) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("حذف جميع الملفات")
                            .setMessage("هل تريد حذف جميع السور المحملة لهذا القارئ؟")
                            .setPositiveButton("حذف الكل") { _, _ ->
                                lifecycleScope.launch {
                                    readerId?.let {
                                        quranListenerReaderViewModel.deleteAllDownloadedAudio(it)

                                        quranListenerReaderAdapter.refreshAllDownloadStatuses()

                                        Snackbar.make(binding.root, "تم حذف جميع الملفات", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .setNegativeButton("إلغاء", null)
                            .show()
                    } else {
                        val pendingCount = totalCount - downloadedCount
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("تحميل السور")
                            .setMessage("يمكن تحميل $pendingCount سورة متبقية من أصل $totalCount")
                            .setPositiveButton("تحميل الكل") { _, _ ->
                                downloadAllSoras()
                            }
                            .setNegativeButton("إلغاء", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "حدث خطأ في تحديد حالة التحميل", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadSingleSura(soraSong: SoraSong) {
        lifecycleScope.launch {
            try {
                val success = readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.downloadAudio(
                        readerId = readerIdValue,
                        surahId = soraSong.SoraId,
                        surahName = SORA_OF_QURAN[soraSong.SoraId] ?: "سورة ${soraSong.SoraId}",
                        readerName = readerName ?: "قارئ",
                        audioUrl = soraSong.url
                    )
                } ?: false

                if (success) {
                    Snackbar.make(binding.root, "بدأ التحميل...", Snackbar.LENGTH_SHORT).show()

                    monitorDownloadCompletion(soraSong.SoraId)
                } else {
                    Snackbar.make(binding.root, "فشل في بدء التحميل", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "حدث خطأ في التحميل", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadAllSoras() {
        lifecycleScope.launch {
            try {
                readerId?.let { readerIdValue ->
                    val success = quranListenerReaderViewModel.downloadAllSoraSongs(
                        readerId = readerIdValue,
                        readerName = readerName ?: "قارئ"
                    ) { current, total ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            quranListenerReaderAdapter.refreshAllDownloadStatuses()
                        }
                    }

                    if (success) {
                        Snackbar.make(binding.root, "بدأ تحميل جميع السور...", Snackbar.LENGTH_LONG).show()
                        monitorBulkDownloadCompletion()
                    } else {
                        Snackbar.make(binding.root, "فشل في بدء التحميل", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "حدث خطأ في التحميل", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun monitorDownloadCompletion(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                repeat(30) {
                    delay(1000)

                    val isDownloaded = readerId?.let {
                        quranListenerReaderViewModel.isAudioDownloaded(it, surahId)
                    } ?: false

                    if (isDownloaded) {
                        withContext(Dispatchers.Main) {
                            quranListenerReaderAdapter.refreshDownloadStatus(surahId)
                            Snackbar.make(binding.root, "تم تحميل السورة بنجاح", Snackbar.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun monitorBulkDownloadCompletion() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                repeat(60) {
                    delay(2000)

                    withContext(Dispatchers.Main) {
                        quranListenerReaderAdapter.refreshAllDownloadStatuses()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun showDeleteDialog(soraSong: SoraSong) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف الملف المحمل")
            .setMessage("هل تريد حذف ${SORA_OF_QURAN[soraSong.SoraId]} المحملة؟")
            .setPositiveButton("حذف") { _, _ ->
                lifecycleScope.launch {
                    readerId?.let {
                        quranListenerReaderViewModel.deleteDownloadedAudio(it, soraSong.SoraId)

                        quranListenerReaderAdapter.refreshDownloadStatus(soraSong.SoraId)

                        Snackbar.make(binding.root, "تم حذف الملف", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }


    private fun showDownloadDialog(soraSong: SoraSong) {
        lifecycleScope.launch {
            val isDownloaded = readerId?.let {
                quranListenerReaderViewModel.isAudioDownloaded(it, soraSong.SoraId)
            } ?: false

            if (isDownloaded) {
                showDeleteDialog(soraSong)
            } else {
                downloadSingleSura(soraSong)
            }
        }
    }


    private fun isFav(isVaForte: Boolean) {
        if (isVaForte) {
            binding.favorite.setImageResource(R.drawable.ic_favorite_red_24)
        } else {
            binding.favorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun launchNotificationPermission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            )
        } else {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!MethodHelper.hasPermissions(requireActivity(), perms)) {
            requestPermissionLauncher.launch(perms)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (!granted) {
            showMessage(requireActivity(), getString(R.string.need_permissions))
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_with_menu_items, menu)
        menu.getItem(1).isVisible = false

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                quranListenerReaderAdapter.filter.filter(newText)
                return false
            }
        })
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