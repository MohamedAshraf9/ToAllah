package com.megahed.eqtarebmenalla.feature_data.presentation.ui.readersList

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.ReadersListAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentReadersListBinding
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReadersListFragment : Fragment(), MenuProvider {

    companion object {
        @JvmStatic
        fun newInstance() = ReadersListFragment()
    }

    private lateinit var binding: FragmentReadersListBinding
    private lateinit var readersListAdapter: ReadersListAdapter

    private val viewModel: ReadersListViewModel by viewModels()

    private var soraId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentReadersListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        soraId = arguments?.let { ReadersListFragmentArgs.fromBundle(it).soraId }

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)



        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        readersListAdapter = ReadersListAdapter(requireContext(), object : OnMyItemClickListener<QuranListenerReader> {

            override fun onItemClick(itemObject: QuranListenerReader, view: View?, position: Int) {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set("readerId", itemObject.id)
                navController.previousBackStackEntry?.savedStateHandle?.set("readerName", itemObject.name)
                navController.navigateUp()
            }

            override fun onItemLongClick(
                itemObject: QuranListenerReader,
                view: View?,
                position: Int
            ) {
            }
        })

        binding.recyclerView.adapter = readersListAdapter

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        toolbar.title = getString(R.string.chooseReader)

        lifecycleScope.launchWhenStarted {
            viewModel.getReadersWithAyatTimings(soraId)
            lifecycleScope.launch {
                viewModel.readersState.collect {
                    if (it.readers.isEmpty()){
                        binding.progressBar.visibility=View.VISIBLE
                        binding.recyclerView.visibility=View.GONE
                    }
                    else{
                        binding.progressBar.visibility=View.GONE
                        binding.recyclerView.visibility=View.VISIBLE
                    }
                    Log.d("ReadersListFragment", "onCreateView: readers & ids: ${it.readers}")
                    readersListAdapter.setData(it.readers)
                }
            }


        }

        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.search_with_menu_items,menu)

        menu[1].isVisible = false

/*        if (fromFavorite)
            menu.getItem(1).isVisible = false*/

        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                readersListAdapter.filter.filter(newText);
                return false
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return  when (menuItem.itemId) {
            /*R.id.moreOptions ->{
                showBottomSheet()
                false
            }*/
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
            }
            else -> false
        }
    }
}