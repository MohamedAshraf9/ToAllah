package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerReaderAdapter
import com.megahed.eqtarebmenalla.common.Constants.SORA_OF_QURAN
import com.megahed.eqtarebmenalla.common.Constants.getSoraLink
import com.megahed.eqtarebmenalla.common.Constants.songs
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerReaderBinding
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener.QuranListenerViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.MainSongsViewModel
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuranListenerReaderFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    //private lateinit var viewModel: QuranListenerReaderViewModel
    private lateinit var quranListenerReaderAdapter :QuranListenerReaderAdapter
    private lateinit var mainViewModel: MainSongsViewModel


    private var readerId:String?=null
    private var readerName:String?=null

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

        quranListenerReaderAdapter= QuranListenerReaderAdapter(requireContext(), object : OnMyItemClickListener<Song> {

            override fun onItemClick(itemObject: Song, view: View?) {
                mainViewModel.playOrToggleSong(itemObject,true)
            }

            override fun onItemLongClick(itemObject: Song, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranListenerReaderAdapter


        songs.clear()

        lifecycleScope.launchWhenStarted {
            readerId?.let {
                quranListenerReaderViewModel.getQuranListenerReaderById(it)?.let {
                    binding.readerName.text=it.name
                    binding.rewaya.text=it.rewaya
                    binding.soraNumbers.text=it.count
                    binding.readerChar.text=it.letter
                }
            }
        }



        quranListenerReaderAdapter.setData(songs)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)




        return root
    }

    private fun getData(suras:String){
        val arr= suras?.split(",")
        val ints= arr?.map { it.toInt() }
        //val songs=mutableListOf<Song>()

        //lifecycleScope.launchWhenStarted {
        ints?.let {
            it.forEach {
                songs.add(Song(
                    it.toString(),
                    SORA_OF_QURAN[it],
                    readerName?:"",
                    getSoraLink(server?:"",it)

                ))
            }
        }
        // }
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