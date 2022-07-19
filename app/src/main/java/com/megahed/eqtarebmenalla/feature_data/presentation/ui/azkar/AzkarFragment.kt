package com.megahed.eqtarebmenalla.feature_data.presentation.ui.azkar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.megahed.eqtarebmenalla.adapter.AyaAdapter
import com.megahed.eqtarebmenalla.adapter.AzkarCategoryAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentAzkarBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran.QuranFragmentDirections
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect


@AndroidEntryPoint
class AzkarFragment : Fragment() {

    private lateinit var binding: FragmentAzkarBinding
    private lateinit var azkarCategoryAdapter: AzkarCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val azkarCategoryAzkarViewModel =
            ViewModelProvider(this).get(AzkarCategoryViewModel::class.java)

        binding = FragmentAzkarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.setHasFixedSize(true)


        azkarCategoryAdapter= AzkarCategoryAdapter(requireContext(), object : OnMyItemClickListener<AzkarCategory> {

            override fun onItemClick(itemObject: AzkarCategory, view: View?) {
                val action: NavDirections = AzkarFragmentDirections.actionNavigationAzkarToElzekrFragment(itemObject.id,itemObject.catName,false)
                Navigation.findNavController(requireView()).navigate(action)
            }

            override fun onItemLongClick(itemObject: AzkarCategory, view: View?) {
            }
        })
        binding.recyclerView.adapter = azkarCategoryAdapter

        lifecycleScope.launchWhenStarted {
            azkarCategoryAzkarViewModel.getAllAzkarCategory().collect{
                azkarCategoryAdapter.setData(it)
            }

        }

        binding.namesOfAllah.setOnClickListener {
            val action: NavDirections = AzkarFragmentDirections.actionNavigationAzkarToNamesOfAllahFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.fabFavorite.setOnClickListener {
            val action: NavDirections = AzkarFragmentDirections.actionNavigationAzkarToElzekrFragment(0,"",true)
            Navigation.findNavController(requireView()).navigate(action)
        }



        return root
    }



}