package com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentAyatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.states.NewTaskSheet
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AyatFragment : Fragment(),MenuProvider {
    private lateinit var binding: FragmentAyatBinding
    private lateinit var quranTextAdapter : AyaAdapter
    private var soraId:Int?=null
    private var soraName:String?=null
    private var fromFavorite:Boolean=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soraId = arguments?.let { AyatFragmentArgs.fromBundle(it).soraId }
        soraName = arguments?.let { AyatFragmentArgs.fromBundle(it).soraName }
        fromFavorite = arguments?.let { AyatFragmentArgs.fromBundle(it).fromFavorite }!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ayaViewModel =
            ViewModelProvider(this).get(AyaViewModel::class.java)

        binding = FragmentAyatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)



        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= AyaAdapter(requireContext(), object : OnMyItemClickListener<Aya> {

            override fun onItemClick(itemObject: Aya, view: View?,position: Int) {
                //fav icon click
                itemObject.isVaForte=!itemObject.isVaForte
                ayaViewModel.updateAya(itemObject)
            }

            override fun onItemLongClick(itemObject: Aya, view: View?,position: Int) {
                val sheet= NewTaskSheet(itemObject,position.toString())
                sheet.show(requireActivity().supportFragmentManager,"sheet")
            }
        })
        binding.recyclerView.adapter = quranTextAdapter
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (!fromFavorite){
            toolbar.title = soraName
            lifecycleScope.launchWhenStarted {

                soraId?.let { it ->
                    ayaViewModel.getAyaOfSoraId(it).collect{it1 ->
                        quranTextAdapter.setData(it1)

                    }
                }
            }


        }else{
            toolbar.title = getString(R.string.favorite)

            lifecycleScope.launchWhenStarted {

                    ayaViewModel.getFavoriteAya().collect{
                        quranTextAdapter.setData(it)

                    }

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