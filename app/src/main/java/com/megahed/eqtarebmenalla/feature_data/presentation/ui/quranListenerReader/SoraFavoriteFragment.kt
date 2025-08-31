package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.SoraFavoriteAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentSoraFavoriteBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.toSong
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener.QuranListenerViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.myListener.OnItemReaderClickListener
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SoraFavoriteFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentSoraFavoriteBinding
    private lateinit var soraFavoriteAdapter : SoraFavoriteAdapter
    private lateinit var mainViewModel: MainSongsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = ViewModelProvider(this).get(MainSongsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val quranListenerViewModel =
            ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)

        binding = FragmentSoraFavoriteBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.title=getString(R.string.soraFav)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)


        soraFavoriteAdapter= SoraFavoriteAdapter(requireContext(), object :
            OnItemReaderClickListener<SoraSong> {

            override fun onItemClickReader(itemObject: SoraSong, view: View?, readerName: String) {
                mainViewModel.playOrToggleSong(itemObject.toSong(readerName.trim()),true)
                val action: NavDirections = SoraFavoriteFragmentDirections.
                actionSoraFavoriteFragmentToSongFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {



            }

            override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                //CommonUtils.showMessage(requireContext(),"${itemObject.id}, ${itemObject.readerId}")
                itemObject.isVaForte=!itemObject.isVaForte
                quranListenerViewModel.updateSoraSong(itemObject)
            }

            override fun onItemLongClick(itemObject: SoraSong, view: View?,position: Int) {

            }
        })
        binding.recyclerView.adapter = soraFavoriteAdapter

        lifecycleScope.launchWhenStarted {
            quranListenerViewModel.getAllFavSorasOfReader().collect{readerList->
                if (readerList.isEmpty()) {
                    binding.loadingContainer.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.lottieView.visibility = View.VISIBLE
                    binding.loadingText.visibility = View.VISIBLE
                } else {
                    binding.loadingContainer.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.lottieView.visibility = View.GONE
                    binding.loadingText.visibility = View.GONE
                }
                soraFavoriteAdapter.setData(readerList)
            }
        }


        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

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