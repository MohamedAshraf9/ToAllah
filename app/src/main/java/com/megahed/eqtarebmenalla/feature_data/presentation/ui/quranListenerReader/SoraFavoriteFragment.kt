package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.SoraFavoriteAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.FragmentSoraFavoriteBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener.QuranListenerViewModel
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoraFavoriteFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentSoraFavoriteBinding
    private lateinit var soraFavoriteAdapter : SoraFavoriteAdapter

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
            OnItemWithFavClickListener<SoraSong> {

            override fun onItemClick(itemObject: SoraSong, view: View?,position: Int) {

                CommonUtils.showMessage(requireContext(),"${itemObject.isVaForte}")

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
            quranListenerViewModel.getAllFavSorasOfReader().collect{
               soraFavoriteAdapter.setData(it)
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