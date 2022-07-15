package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.QuranListenerAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentQuranListenerBinding
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class QuranListenerFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentQuranListenerBinding
    private lateinit var quranListenerAdapter : QuranListenerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val quranListenerViewModel =
            ViewModelProvider(this).get(QuranListenerViewModel::class.java)

        binding = FragmentQuranListenerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.listener)

        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerAdapter= QuranListenerAdapter(requireContext(), object : OnMyItemClickListener<Reciter> {

            override fun onItemClick(itemObject: Reciter, view: View?) {
                //val action: NavDirections = QuranFragmentDirections.actionNavigationQuranToAyatFragment(itemObject.soraId,itemObject.name)
                //Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemLongClick(itemObject: Reciter, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranListenerAdapter

        quranListenerViewModel.getQuranData()
        lifecycleScope.launchWhenStarted {

            quranListenerViewModel.state.collect{
                quranListenerAdapter.setData(it.reciter)
            }


        }

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

                quranListenerAdapter.filter.filter(newText);
                return false
            }
        })

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return false

    }



}