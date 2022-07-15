package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
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
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.SoraAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentQuranBinding
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class QuranFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentQuranBinding
    private lateinit var quranTextAdapter :SoraAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val soraViewModel =
            ViewModelProvider(this).get(SoraViewModel::class.java)

        binding = FragmentQuranBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.quran)

        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= SoraAdapter(requireContext(), object : OnMyItemClickListener<Sora>{

            override fun onItemClick(itemObject: Sora, view: View?) {
                val action: NavDirections =QuranFragmentDirections.actionNavigationQuranToAyatFragment(itemObject.soraId,itemObject.name)
                Navigation.findNavController(requireView()).navigate(action)
                //Toast.makeText(requireContext(),itemObject.name,Toast.LENGTH_LONG).show()
            }

            override fun onItemLongClick(itemObject: Sora, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranTextAdapter
        lifecycleScope.launchWhenStarted {

            soraViewModel.getAllSora().collect{
                quranTextAdapter.setData(it)

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

                quranTextAdapter.filter.filter(newText);
                return false
            }
        })

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return false

    }


}