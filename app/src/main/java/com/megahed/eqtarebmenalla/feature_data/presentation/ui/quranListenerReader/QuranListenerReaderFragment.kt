package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.EditorInfo
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
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerReaderAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.common.Constants.SORA_OF_QURAN
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerReaderBinding
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.toSong
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import com.megahed.eqtarebmenalla.offline.OfflineUtils.isNetworkAvailable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.findNavController
import androidx.core.view.get

@AndroidEntryPoint
class QuranListenerReaderFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    private lateinit var quranListenerReaderAdapter: QuranListenerReaderAdapter
    private lateinit var mainViewModel: MainSongsViewModel
    private lateinit var quranListenerReaderViewModel: QuranListenerReaderViewModel

    private var readerId: String? = null
    private var readerName: String? = null
    private var quranListenerReader: QuranListenerReader? = null

    private val quranReaderSoras = mutableListOf<SoraSong>()
    private lateinit var offlineAudioManager: OfflineAudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readerId = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).id }
        readerName = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).readerName }

        mainViewModel = ViewModelProvider(this)[MainSongsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        quranListenerReaderViewModel =
            ViewModelProvider(this)[QuranListenerReaderViewModel::class.java]
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

    private fun showOfflineAlert() {
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
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private suspend fun isSurahDownloaded(soraSong: SoraSong): Boolean {
        return withContext(Dispatchers.IO) {
            readerId?.let {
                quranListenerReaderViewModel.isAudioDownloaded(it, soraSong.SoraId)
            } ?: false
        }
    }

    private fun setupUI() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }

        var isShow = true
        var scrollRange = -1
        binding.appBar.addOnOffsetChangedListener { barLayout, verticalOffset ->
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
        }
    }

    private fun setupRecyclerView() {
        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerReaderAdapter = QuranListenerReaderAdapter(
            requireContext(),
            object : OnItemWithFavClickListener<SoraSong> {
                override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {
                    lifecycleScope.launch {
                        try {
                            if (!isNetworkAvailable(requireContext()) && !isSurahDownloaded(
                                    itemObject
                                )
                            ) {
                                showOfflineAlert()
                                return@launch
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launchNotificationPermission()
                            }

                            val song = itemObject.toSong(readerName)
                            mainViewModel.playOrToggleSong(song, true)

                            val action: NavDirections =
                                QuranListenerReaderFragmentDirections.actionQuranListenerReaderFragmentToSongFragment()
                            requireView().findNavController().navigate(action)
                        } catch (_: Exception) {
                            Snackbar.make(
                                binding.root,
                                "حدث خطأ في تشغيل السورة",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                    try {
                        itemObject.isVaForte = !itemObject.isVaForte
                        quranListenerReaderViewModel.updateSoraSong(itemObject)
                    } catch (_: Exception) {

                    }
                }

                override fun onItemLongClick(itemObject: SoraSong, view: View?, position: Int) {
                    lifecycleScope.launch {
                        try {
                            showDownloadDialog(itemObject)
                        } catch (_: Exception) {

                        }
                    }
                }
            },
            offlineAudioManager,
            readerId ?: "",
            lifecycleScope
        )
        binding.recyclerView.adapter = quranListenerReaderAdapter
    }


    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.getSongsOfSora(readerIdValue)
                        .collect { soraSongs ->
                            withContext(Dispatchers.Main) {
                                try {
                                    FirebaseMusicSource._audiosLiveData.value =
                                        soraSongs.map { it.toSong(readerName) }
                                    synchronized(quranReaderSoras) {
                                        quranReaderSoras.clear()
                                        quranReaderSoras.addAll(soraSongs)
                                    }
                                    quranListenerReaderAdapter.setData(soraSongs)

                                } catch (_: Exception) {
                                }
                            }
                        }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quranListenerReaderViewModel.downloadProgress.collect { progressMap ->
                    withContext(Dispatchers.Main) {
                        quranListenerReaderAdapter.updateDownloadProgress(progressMap)
                    }
                }
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

    private fun showBulkDownloadDialog() {
        lifecycleScope.launch {
            try {
                val (downloadedCount, totalCount) = withContext(Dispatchers.IO) {
                    val readerIdValue = readerId ?: return@withContext Pair(0, 0)

                    val sorasCopy = synchronized(quranReaderSoras) {
                        quranReaderSoras.toList()
                    }

                    val downloaded = sorasCopy.count { soraSong ->
                        try {
                            quranListenerReaderViewModel.isAudioDownloaded(
                                readerIdValue,
                                soraSong.SoraId
                            )
                        } catch (_: Exception) {

                            false
                        }
                    }

                    Pair(downloaded, sorasCopy.size)
                }

                withContext(Dispatchers.Main) {
                    if (downloadedCount == totalCount && totalCount > 0) {
                        showDeleteAllDialog(totalCount)
                    } else {
                        showDownloadAllDialog(downloadedCount, totalCount)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "حدث خطأ في تحديد حالة التحميل",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showDeleteAllDialog(totalCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف جميع الملفات")
            .setMessage("هل تريد حذف جميع السور المحملة ($totalCount سورة) لهذا القارئ؟")
            .setPositiveButton("حذف الكل") { _, _ ->
                lifecycleScope.launch {
                    try {
                        readerId?.let { readerIdValue ->
                            quranListenerReaderViewModel.deleteAllDownloadedAudio(readerIdValue)
                            quranListenerReaderAdapter.refreshAllDownloadStatuses()
                            Snackbar.make(
                                binding.root,
                                "تم حذف جميع الملفات",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    } catch (_: Exception) {
                        Snackbar.make(binding.root, "حدث خطأ في حذف الملفات", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showDownloadAllDialog(downloadedCount: Int, totalCount: Int) {
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


    private fun downloadSingleSura(soraSong: SoraSong) {
        lifecycleScope.launch {
            try {
                val success = readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.downloadAudio(
                        readerId = readerIdValue,
                        surahId = soraSong.SoraId,
                        surahName = SORA_OF_QURAN[soraSong.SoraId],
                        readerName = readerName ?: "قارئ",
                        audioUrl = soraSong.url
                    )
                } ?: false

                if (success) {
                    if (isNetworkAvailable(requireContext())) {
                        Snackbar.make(binding.root, "بدأ التحميل...", Snackbar.LENGTH_SHORT).show()
                        monitorDownloadCompletion(soraSong.SoraId)
                    } else {
                        Snackbar.make(
                            binding.root,
                            "انت غير متصل بالإنترنت, سيتم التحميل تقائياً عند توفر إتصال بالإنترنت",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        monitorDownloadCompletion(soraSong.SoraId)
                    }

                } else {
                    Snackbar.make(binding.root, "فشل في بدء التحميل", Snackbar.LENGTH_LONG)
                        .show()
                }
            } catch (_: Exception) {
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
                            try {
                                quranListenerReaderAdapter.refreshAllDownloadStatuses()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    if (success) {
                        if (isNetworkAvailable(requireContext())) {
                            Snackbar.make(
                                binding.root,
                                "بدأ تحميل جميع السور...",
                                Snackbar.LENGTH_LONG
                            ).show()
                            monitorBulkDownloadCompletion()
                        } else {
                            Snackbar.make(
                                binding.root,
                                "انت غير متصل بالإنترنت, سيتم التحميل تلقائياً عند وجود إتصال",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Snackbar.make(binding.root, "فشل في بدء التحميل", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            } catch (_: Exception) {
                Snackbar.make(binding.root, "حدث خطأ في التحميل", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun monitorBulkDownloadCompletion() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var attempts = 0
                val maxAttempts = 60 // 2 minutes maximum

                while (attempts < maxAttempts) {
                    delay(2000) // Check every 2 seconds
                    attempts++

                    try {
                        withContext(Dispatchers.Main) {
                            quranListenerReaderAdapter.refreshAllDownloadStatuses()
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (_: Exception) {

            }
        }
    }

    private fun monitorDownloadCompletion(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var attempts = 0
                val maxAttempts = 30 // 30 seconds maximum

                while (attempts < maxAttempts) {
                    delay(1000) // Check every second
                    attempts++

                    val isDownloaded = readerId?.let { readerIdValue ->
                        try {
                            quranListenerReaderViewModel.isAudioDownloaded(readerIdValue, surahId)
                        } catch (_: Exception) {

                            false
                        }
                    } ?: false

                    if (isDownloaded) {
                        withContext(Dispatchers.Main) {
                            try {
                                quranListenerReaderAdapter.refreshDownloadStatus(surahId)
                                Snackbar.make(
                                    binding.root,
                                    "تم تحميل السورة بنجاح",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } catch (_: Exception) {

                            }
                        }
                        return@launch
                    }
                }
            } catch (_: Exception) {
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


    private suspend fun showDownloadDialog(soraSong: SoraSong) {
        val isDownloaded = readerId?.let {
            quranListenerReaderViewModel.isAudioDownloaded(it, soraSong.SoraId)
        } ?: false

        withContext(Dispatchers.Main) {
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
        menu[1].isVisible = false

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
                requireView().findNavController().popBackStack()
            }

            else -> false
        }
    }
}