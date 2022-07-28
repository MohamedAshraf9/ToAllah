package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
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
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class QuranListenerFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentQuranListenerBinding
    private lateinit var quranListenerAdapter : QuranListenerAdapter
    private var fromFavorite:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromFavorite = arguments?.let { QuranListenerFragmentArgs.fromBundle(it).fromFavorite }!!
    }

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



        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranListenerAdapter= QuranListenerAdapter(requireContext(), object : OnItemWithFavClickListener<QuranListenerReader> {

            override fun onItemClick(itemObject: QuranListenerReader, view: View?) {
                val action: NavDirections = QuranListenerFragmentDirections.
                actionNavigationListenerToQuranListenerReaderFragment(
                  id = itemObject.id,
                  readerName = itemObject.name
                )
                Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemFavClick(itemObject: QuranListenerReader, view: View?) {
                itemObject.isVaForte=!itemObject.isVaForte
                quranListenerViewModel.updateQuranListenerReader(itemObject)
            }

            override fun onItemLongClick(itemObject: QuranListenerReader, view: View?) {
            }
        })
        binding.recyclerView.adapter = quranListenerAdapter
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (!fromFavorite) {
            toolbar.title = getString(R.string.listener)

            lifecycleScope.launchWhenStarted {
                quranListenerViewModel.getAllQuranListenerReader().collect {
                    quranListenerAdapter.setData(it)
                }


            }

          /*  lifecycleScope.launchWhenStarted {
                quranListenerViewModel.getAllFavSorasOfReader().collect{
                    it.map {
                        it.soraSongData.map {
                            it.isVaForte
                        }
                    }
                    it.forEach {
                        Log.d("jkhdssjkdhsjhd",it.quranListenerReader.name)
                        Log.d("jkhdssjkdhsjhd","${it.soraSongData.size}")

                    }
                }
            }*/

        }
        else{
            (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
            (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

            lifecycleScope.launchWhenStarted {
                quranListenerViewModel.getFavoriteQuranListenerReader().collect {
                    quranListenerAdapter.setData(it)
                }


            }

        }






        return root
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

        menuInflater.inflate(R.menu.search_with_menu_items,menu)

        if (fromFavorite)
            menu.getItem(1).isVisible = false

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
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
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

            bottomSheetDialog.dismiss()
            val action: NavDirections = QuranListenerFragmentDirections.
            actionNavigationListenerSelf(true)
            Navigation.findNavController(requireView()).navigate(action)

        }

        soraFav.setOnClickListener {
            bottomSheetDialog.dismiss()
            val action: NavDirections = QuranListenerFragmentDirections.
            actionNavigationListenerToSoraFavoriteFragment()
            Navigation.findNavController(requireView()).navigate(action)


        }
        listeningToSave.setOnClickListener {
            bottomSheetDialog.dismiss()

        }


        //code
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.dismissWithAnimation = true
        bottomSheetDialog.window?.attributes?.windowAnimations =
            R.style.DialogAnimation

        bottomSheetDialog.show()
    }


}