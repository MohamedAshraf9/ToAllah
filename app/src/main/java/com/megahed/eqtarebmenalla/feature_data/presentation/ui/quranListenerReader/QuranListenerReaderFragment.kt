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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class QuranListenerReaderFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    //private lateinit var viewModel: QuranListenerReaderViewModel
    private lateinit var quranListenerReaderAdapter :QuranListenerReaderAdapter
    private lateinit var mainViewModel: MainSongsViewModel


    private var readerId:String?=null
    private var readerName:String?=null
    private var quranListenerReader: QuranListenerReader?=null

    private val quranReaderSoras: ArrayList<SoraSong> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readerId = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).id }
        readerName = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).readerName }

        //viewModel = ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)
        mainViewModel = ViewModelProvider(this).get(MainSongsViewModel::class.java)
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val quranListenerReaderViewModel =
            ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)

        val toolbar: Toolbar = binding.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launchWhenStarted {
            readerId?.let {
                quranListenerReaderViewModel.getQuranListenerReaderById(it)?.let {
                    binding.readerName.text=it.name
                    binding.rewaya.text=it.rewaya
                    binding.soraNumbers.text=it.count
                    binding.readerChar.text=it.letter
                    isFav(it.isVaForte)
                    quranListenerReader=it

                }
            }
        }


        var isShow = true
        var scrollRange = -1
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1){
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0){
                binding.toolbarLayout.title =readerName
                binding.toolbar.title = readerName
                isShow = true
            } else if (isShow){
                binding.toolbarLayout.title = " " //careful there should a space between double quote otherwise it wont work
                binding.toolbar.title = " " //careful there should a space between double quote otherwise it wont work
                isShow = false
            }
        })


        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerReaderAdapter= QuranListenerReaderAdapter(requireContext(), object :
            OnItemWithFavClickListener<SoraSong> {

            override fun onItemClick(itemObject: SoraSong, view: View?,position: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launchNotificationPermission()
                }
                mainViewModel.playOrToggleSong(itemObject.toSong(readerName),true)
                val action: NavDirections = QuranListenerReaderFragmentDirections.
                actionQuranListenerReaderFragmentToSongFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                itemObject.isVaForte=!itemObject.isVaForte
                quranListenerReaderViewModel.updateSoraSong(itemObject)
            }

            override fun onItemLongClick(itemObject: SoraSong, view: View?,position: Int) {
                downloadTask(itemObject)
            }
        })
        binding.recyclerView.adapter = quranListenerReaderAdapter



        lifecycleScope.launchWhenStarted {
            readerId?.let {
                quranListenerReaderViewModel.getSongsOfSora(it).collect{
                    FirebaseMusicSource._audiosLiveData.value=it.map { it.toSong(readerName) }
                    quranListenerReaderAdapter.setData(it)
                    quranReaderSoras.clear()
                    quranReaderSoras.addAll(it)
                }
            }
        }

        binding.favorite.setOnClickListener {
            quranListenerReader?.let {
                it.isVaForte=!it.isVaForte
                quranListenerReaderViewModel.updateQuranListenerReader(it)
                isFav(it.isVaForte)
            }


        }

        binding.download.setOnClickListener {
            downloadAllSoraSongs(quranReaderSoras, readerName.toString())
        }

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)




        return root
    }


    private fun isFav(isVaForte:Boolean){
        if (isVaForte){
            binding.favorite.setImageResource(R.drawable.ic_favorite_red_24)
        }
        else{
            binding.favorite.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }
    }

    private fun getData(suras:String,server:String){
        val arr= suras.split(",")
        val ints= arr.map { it.toInt() }
        ints.let {
            it.forEach {
                songs.add(Song(
                    it.toString(),
                    SORA_OF_QURAN[it],
                    readerName?:"",
                    getSoraLink(server,it)

                ))
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun launchNotificationPermission(){
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
            )
        } else {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        if (!MethodHelper.hasPermissions(requireActivity(), perms)){
            requestPermissionLauncher.launch(perms)
        }

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        // }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all {
            it.value
        }
        if (!granted) {
            // PERMISSION GRANTED
            showMessage(requireActivity(),getString(R.string.need_permissions))
        }
    }

    @Throws(Exception::class)
    private fun downloadTask(soraSong: SoraSong): Boolean {
        if (!soraSong.url.startsWith("http")) {
            MethodHelper.toastMessage(getString(R.string.error))
            return false
        }
        try {

            val file= File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS + File.separator + readerName)
            if (!file.exists()) {
                file.mkdirs()
            }
            val result = File(file.absolutePath + File.separator.toString() + SORA_OF_QURAN[soraSong.SoraId]+"_"+(readerName?:"")+"."+soraSong.url.substringAfterLast('.', ""))
            val downloadManager = requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(soraSong.url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            request.setDestinationUri(Uri.fromFile(result))
           // request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS + File.separator + readerName, result.name)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadManager.enqueue(request)
            MethodHelper.toastMessage(getString(R.string.downloading))
            MediaScannerConnection.scanFile(
                context, arrayOf(result.toString()), null
            ) { path, uri ->
                MethodHelper.toastMessage(path)
            }


            //Log.d("jhdfgdjf", "yyyyyyyyyyyyyy")
        } catch (e: Exception) {
            MethodHelper.toastMessage(getString(R.string.error))
            //e.message?.let { Log.d("jhdfgdjf", it) }
            return false
        }
        return true
    }

    @Throws(Exception::class)
    private fun downloadAllSoraSongs(soraSongs: List<SoraSong>, folderName: String): Boolean {
        try {
            // Create a folder in the Downloads directory with the specific folder name
            val directory = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS + File.separator + folderName)
            if (!directory.exists()) {
                directory.mkdirs()  // Create the folder if it doesn't exist
            }

            val downloadManager = requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Use a coroutine scope to throttle the download requests
            CoroutineScope(Dispatchers.IO).launch {
                // Loop through all SoraSongs and download them
                for (soraSong in soraSongs) {
                    if (!soraSong.url.startsWith("http")) {
                        withContext(Dispatchers.Main) {
                            MethodHelper.toastMessage(getString(R.string.error))
                        }
                        // Skip to the next file if the URL is invalid
                        continue
                    }

                    // Create the file path for each download
                    val result = File(
                        directory.absolutePath + File.separator + SORA_OF_QURAN[soraSong.SoraId] + "_" + (readerName
                            ?: "") + "." + soraSong.url.substringAfterLast('.', "")
                    )

                    val request = DownloadManager.Request(Uri.parse(soraSong.url))
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                    request.setDestinationUri(Uri.fromFile(result))
                    //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS + File.separator + folderName, result.name)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                    // Enqueue each download request
                    downloadManager.enqueue(request)

                    // Introduce a delay between downloads (e.g., 500 milliseconds)
                    delay(500)
                }

                withContext(Dispatchers.Main) {
                    MethodHelper.toastMessage(getString(R.string.downloading_all_files))
                }
            }
            return true

        } catch (e: Exception) {
            MethodHelper.toastMessage(getString(R.string.error))
            Log.d("k", "downloadAllSoraSongs: ${e.message}")
            return false
        }
    }



    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

        menuInflater.inflate(R.menu.search_with_menu_items,menu)


        menu.getItem(1).isVisible = false

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                quranListenerReaderAdapter.filter.filter(newText);
                return false
            }
        })

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return  when (menuItem.itemId) {
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
            }
            else -> false
        }
    }




}