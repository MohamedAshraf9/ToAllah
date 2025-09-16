package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
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
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap

@AndroidEntryPoint
class QuranListenerReaderFragment : Fragment(), MenuProvider {

    private var _binding: FragmentQuranListenerReaderBinding? = null
    private val binding get() = _binding!!

    private lateinit var quranListenerReaderAdapter: QuranListenerReaderAdapter
    private lateinit var mainViewModel: MainSongsViewModel
    private lateinit var quranListenerReaderViewModel: QuranListenerReaderViewModel

    private var readerId: String? = null
    private var readerName: String? = null
    private var quranListenerReader: QuranListenerReader? = null

    private val quranReaderSoras = ConcurrentHashMap<Int, SoraSong>()
    private lateinit var offlineAudioManager: OfflineAudioManager

    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private var pendingFavUpdate: Runnable? = null
    private var pendingDownloadUpdate: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            readerId = QuranListenerReaderFragmentArgs.fromBundle(args).id
            readerName = QuranListenerReaderFragmentArgs.fromBundle(args).readerName
        }

        mainViewModel = ViewModelProvider(this)[MainSongsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)

        quranListenerReaderViewModel = ViewModelProvider(this)[QuranListenerReaderViewModel::class.java]
        offlineAudioManager = quranListenerReaderViewModel.getOfflineAudioManager()

        setupToolbar()
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onDestroyView() {

        pendingFavUpdate?.let { uiUpdateHandler.removeCallbacks(it) }
        pendingDownloadUpdate?.let { uiUpdateHandler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar
        (activity as AppCompatActivity?)?.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
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
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private suspend fun isSurahDownloaded(soraSong: SoraSong): Boolean {
        return readerId?.let {
            quranListenerReaderViewModel.isSurahDownloadedCached(it, soraSong.SoraId)
        } ?: false
    }

    private fun setupUI() {
        readerId?.let { id ->

            lifecycleScope.launch(Dispatchers.IO) {
                val reader = quranListenerReaderViewModel.getQuranListenerReaderById(id)

                withContext(Dispatchers.Main) {
                    reader?.let { readerData ->
                        binding.readerName.text = readerData.name
                        binding.rewaya.text = readerData.rewaya
                        binding.soraNumbers.text = readerData.count
                        binding.readerChar.text = readerData.letter
                        updateFavoriteIcon(readerData.isVaForte)
                        quranListenerReader = readerData
                    }
                }
            }
        }

        var isShow = true
        var scrollRange = -1
        binding.appBar.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange ?: 0
            }
            if (scrollRange + verticalOffset == 0) {
                if (!isShow) {
                    binding.toolbarLayout.title = readerName
                    binding.toolbar.title = readerName
                    isShow = true
                }
            } else if (isShow) {
                binding.toolbarLayout.title = " "
                binding.toolbar.title = " "
                isShow = false
            }
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)

            itemAnimator = DefaultItemAnimator().apply {
                changeDuration = 150
                addDuration = 150
                removeDuration = 150
            }
        }

        quranListenerReaderAdapter = QuranListenerReaderAdapter(
            context = requireContext(),
            onItemClickListener = createItemClickListener(),
            offlineAudioManager = offlineAudioManager,
            readerId = readerId ?: "",
            lifecycleScope = lifecycleScope
        )

        binding.recyclerView.adapter = quranListenerReaderAdapter
    }

    private fun createItemClickListener() = object : OnItemWithFavClickListener<SoraSong> {
        override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {
            lifecycleScope.launch {
                try {
                    if (!isNetworkAvailable(requireContext()) && !isSurahDownloaded(itemObject)) {
                        showOfflineAlert()
                        return@launch
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launchNotificationPermission()
                    }

                    val song = itemObject.toSong(readerName)
                    mainViewModel.playOrToggleSong(song, true)

                    val action: NavDirections = QuranListenerReaderFragmentDirections
                        .actionQuranListenerReaderFragmentToSongFragment()
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    showErrorSnackbar("حدث خطأ في تشغيل السورة")
                }
            }
        }

        override fun onItemFavClick(itemObject: SoraSong, view: View?) {

            handleFavoriteClick(itemObject)
        }

        override fun onItemLongClick(itemObject: SoraSong, view: View?, position: Int) {
            lifecycleScope.launch {
                try {
                    showDownloadDialog(itemObject)
                } catch (e: Exception) {
                    showErrorSnackbar("حدث خطأ في إدارة التحميل")
                }
            }
        }
    }

    private fun handleFavoriteClick(soraSong: SoraSong) {

        pendingFavUpdate?.let { uiUpdateHandler.removeCallbacks(it) }

        soraSong.isVaForte = !soraSong.isVaForte
        quranListenerReaderAdapter.updateFavoriteStatus(soraSong.SoraId, soraSong.isVaForte)

        pendingFavUpdate = Runnable {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    quranListenerReaderViewModel.updateSoraSongFavorite(soraSong)
                } catch (e: Exception) {

                    withContext(Dispatchers.Main) {
                        soraSong.isVaForte = !soraSong.isVaForte
                        quranListenerReaderAdapter.updateFavoriteStatus(soraSong.SoraId, soraSong.isVaForte)
                        showErrorSnackbar("فشل في تحديث المفضلة")
                    }
                }
            }
        }
        uiUpdateHandler.postDelayed(pendingFavUpdate!!, 300)
    }

    private fun setupObservers() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.getSongsOfSoraCached(readerIdValue)
                        .flowOn(Dispatchers.IO)
                        .collect { soraSongs ->
                            withContext(Dispatchers.Main) {
                                try {

                                    FirebaseMusicSource._audiosLiveData.value =
                                        soraSongs.map { it.toSong(readerName) }

                                    quranReaderSoras.clear()
                                    soraSongs.forEach { song ->
                                        quranReaderSoras[song.SoraId] = song
                                    }

                                    quranListenerReaderAdapter.submitSoraList(soraSongs)
                                } catch (e: Exception) {
                                    showErrorSnackbar("خطأ في تحميل البيانات")
                                }
                            }
                        }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                quranListenerReaderViewModel.downloadProgress
                    .collect { progressMap ->
                        withContext(Dispatchers.Main) {
                            quranListenerReaderAdapter.updateDownloadProgress(progressMap)
                        }
                    }
            }
        }
    }

    private fun setupClickListeners() {
        binding.favorite.setOnClickListener {
            quranListenerReader?.let { reader ->
                reader.isVaForte = !reader.isVaForte
                updateFavoriteIcon(reader.isVaForte)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        quranListenerReaderViewModel.updateQuranListenerReaderFavorite(reader)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            reader.isVaForte = !reader.isVaForte
                            updateFavoriteIcon(reader.isVaForte)
                            showErrorSnackbar("فشل في تحديث المفضلة")
                        }
                    }
                }
            }
        }

        binding.download.setOnClickListener {
            showBulkDownloadDialog()
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        val iconRes = if (isFavorite) {
            R.drawable.ic_favorite_red_24
        } else {
            R.drawable.ic_baseline_favorite_border_24
        }
        binding.favorite.setImageResource(iconRes)
    }

    private fun showBulkDownloadDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val readerIdValue = readerId ?: return@launch
                val sorasList = quranReaderSoras.values.toList()

                if (sorasList.isEmpty()) return@launch

                val downloadStatuses = quranListenerReaderViewModel
                    .getBulkDownloadStatuses(readerIdValue, sorasList.map { it.SoraId })

                val downloadedCount = downloadStatuses.count { it }
                val totalCount = sorasList.size

                withContext(Dispatchers.Main) {
                    if (downloadedCount == totalCount && totalCount > 0) {
                        showDeleteAllDialog(totalCount)
                    } else {
                        showDownloadAllDialog(downloadedCount, totalCount)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorSnackbar("حدث خطأ في تحديد حالة التحميل")
                }
            }
        }
    }

    private fun showDeleteAllDialog(totalCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف جميع الملفات")
            .setMessage("هل تريد حذف جميع السور المحملة ($totalCount سورة) لهذا القارئ؟")
            .setPositiveButton("حذف الكل") { _, _ ->
                performDeleteAll()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun performDeleteAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.deleteAllDownloadedAudioOptimized(readerIdValue)

                    withContext(Dispatchers.Main) {
                        quranListenerReaderAdapter.refreshAllDownloadStatuses()
                        showSuccessSnackbar("تم حذف جميع الملفات")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorSnackbar("حدث خطأ في حذف الملفات")
                }
            }
        }
    }

    private fun showDownloadAllDialog(downloadedCount: Int, totalCount: Int) {
        val pendingCount = totalCount - downloadedCount
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("تحميل السور")
            .setMessage("يمكن تحميل $pendingCount سورة متبقية من أصل $totalCount")
            .setPositiveButton("تحميل الكل") { _, _ ->
                performDownloadAll()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun performDownloadAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                readerId?.let { readerIdValue ->
                    val success = quranListenerReaderViewModel.downloadAllSoraSongsOptimized(
                        readerId = readerIdValue,
                        readerName = readerName ?: "قارئ"
                    )

                    withContext(Dispatchers.Main) {
                        if (success) {
                            val message = if (isNetworkAvailable(requireContext())) {
                                "بدأ تحميل جميع السور..."
                            } else {
                                "انت غير متصل بالإنترنت, سيتم التحميل تلقائياً عند وجود إتصال"
                            }
                            showSuccessSnackbar(message)
                            startDownloadMonitoring()
                        } else {
                            showErrorSnackbar("فشل في بدء التحميل")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorSnackbar("حدث خطأ في التحميل")
                }
            }
        }
    }

    private suspend fun showDownloadDialog(soraSong: SoraSong) {
        val isDownloaded = readerId?.let {
            quranListenerReaderViewModel.isSurahDownloadedCached(it, soraSong.SoraId)
        } ?: false

        withContext(Dispatchers.Main) {
            if (isDownloaded) {
                showDeleteDialog(soraSong)
            } else {
                downloadSingleSura(soraSong)
            }
        }
    }

    private fun showDeleteDialog(soraSong: SoraSong) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("حذف الملف المحمل")
            .setMessage("هل تريد حذف ${SORA_OF_QURAN[soraSong.SoraId]} المحملة؟")
            .setPositiveButton("حذف") { _, _ ->
                performSingleDelete(soraSong)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun performSingleDelete(soraSong: SoraSong) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.deleteDownloadedAudioOptimized(readerIdValue, soraSong.SoraId)

                    withContext(Dispatchers.Main) {
                        quranListenerReaderAdapter.refreshDownloadStatus(soraSong.SoraId)
                        showSuccessSnackbar("تم حذف الملف")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorSnackbar("فشل في حذف الملف")
                }
            }
        }
    }

    private fun downloadSingleSura(soraSong: SoraSong) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val success = readerId?.let { readerIdValue ->
                    quranListenerReaderViewModel.downloadAudioOptimized(
                        readerId = readerIdValue,
                        surahId = soraSong.SoraId,
                        surahName = SORA_OF_QURAN[soraSong.SoraId],
                        readerName = readerName ?: "قارئ",
                        audioUrl = soraSong.url
                    )
                } ?: false

                withContext(Dispatchers.Main) {
                    if (success) {
                        val message = if (isNetworkAvailable(requireContext())) {
                            "بدأ التحميل..."
                        } else {
                            "انت غير متصل بالإنترنت, سيتم التحميل تلقائياً عند توفر إتصال بالإنترنت"
                        }
                        showSuccessSnackbar(message)

                        monitorSingleDownload(soraSong.SoraId)
                    } else {
                        showErrorSnackbar("فشل في بدء التحميل")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorSnackbar("حدث خطأ في التحميل")
                }
            }
        }
    }

    private fun startDownloadMonitoring() {
        lifecycleScope.launch(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                delay(2000)
                attempts++

                try {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            quranListenerReaderAdapter.refreshAllDownloadStatuses()
                        }
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun monitorSingleDownload(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 30

            while (attempts < maxAttempts) {
                delay(1000)
                attempts++

                val isDownloaded = readerId?.let { readerIdValue ->
                    try {
                        quranListenerReaderViewModel.isSurahDownloadedCached(readerIdValue, surahId)
                    } catch (e: Exception) {
                        false
                    }
                } ?: false

                if (isDownloaded) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            quranListenerReaderAdapter.refreshDownloadStatus(surahId)
                            showSuccessSnackbar("تم تحميل السورة بنجاح")
                        }
                    }
                    return@launch
                }
            }
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

    private fun showErrorSnackbar(message: String) {
        if (_binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_red))
                .show()
        }
    }

    private fun showSuccessSnackbar(message: String) {
        if (_binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_green))
                .show()
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
                findNavController().popBackStack()
                true
            }
            else -> false
        }
    }
}