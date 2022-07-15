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
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerReaderAdapter
import com.megahed.eqtarebmenalla.adapter.SoraAdapter
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerReaderBinding
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyatFragmentArgs
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran.QuranFragmentDirections
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class QuranListenerReaderFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentQuranListenerReaderBinding
    private lateinit var viewModel: QuranListenerReaderViewModel
    private lateinit var quranListenerReaderAdapter :QuranListenerReaderAdapter


    private var readerName:String?=null
    private var suras:String?=null
    private var letter:String?=null
    private var count:String?=null
    private var rewaya:String?=null
    private var server:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readerName = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).readerName }
        suras = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).suras }
        letter = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).letter }
        count = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).count }
        rewaya = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).rewaya }
        server = arguments?.let { QuranListenerReaderFragmentArgs.fromBundle(it).server }

        viewModel = ViewModelProvider(this).get(QuranListenerReaderViewModel::class.java)
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuranListenerReaderBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val toolbar: Toolbar = binding.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding.readerName.text=readerName
        binding.rewaya.text=rewaya
        binding.soraNumbers.text=count
        binding.readerChar.text=letter



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

        quranListenerReaderAdapter= QuranListenerReaderAdapter(requireContext(), object : OnMyItemClickListener<Int> {

            override fun onItemClick(itemObject: Int, view: View?) {

            }

            override fun onItemLongClick(itemObject: Int, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranListenerReaderAdapter

        val arr= suras?.split(",")
        val ints= arr?.map { it.toInt() }
        ints?.let { quranListenerReaderAdapter.setData(it) }

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)




        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

        menuInflater.inflate(R.menu.quran_listener_menu,menu)
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