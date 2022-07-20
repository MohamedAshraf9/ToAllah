package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
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
                val action: NavDirections = QuranListenerFragmentDirections.
                actionNavigationListenerToQuranListenerReaderFragment(
                    readerName = itemObject.name,
                    suras = itemObject.suras,
                    letter = itemObject.letter,
                    count = itemObject.count,
                    rewaya = itemObject.rewaya,
                    server = itemObject.server
                )
                Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemLongClick(itemObject: Reciter, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranListenerAdapter
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        quranListenerViewModel.getQuranData()
        lifecycleScope.launchWhenStarted {

            quranListenerViewModel.state.collect{
                quranListenerAdapter.setData(it.reciter)
            }


        }






        return root
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

        menuInflater.inflate(R.menu.search_menu,menu)
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

        return  when (menuItem.itemId) {
            R.id.moreOptions ->{
              showBottomSheet()
                false
            }
            else -> false
        }

    }


    private fun showBottomSheet(){
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView: View = LayoutInflater.from(
            requireActivity()
        ).inflate(
            R.layout.bottom_sheet_listener,
            requireView().findViewById<ConstraintLayout>(R.id.container2)
        )

        val readerFav= bottomSheetView.findViewById<ImageView>(R.id.readerFav)
        val soraFav= bottomSheetView.findViewById<ImageView>(R.id.soraFav)
        val listeningToSave= bottomSheetView.findViewById<ImageView>(R.id.listeningToSave)

        readerFav.setOnClickListener {

        }
        soraFav.setOnClickListener {

        }
        listeningToSave.setOnClickListener {

        }


        //code
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.dismissWithAnimation = true
        bottomSheetDialog.window?.attributes?.windowAnimations =
            R.style.DialogAnimation

        bottomSheetDialog.show()
    }


}