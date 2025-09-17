package com.megahed.eqtarebmenalla.feature_data.presentation.ui.azkar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.adapter.AzkarCategoryAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentAzkarBinding
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.findNavController


@AndroidEntryPoint
class AzkarFragment : Fragment() {

    private lateinit var binding: FragmentAzkarBinding
    private lateinit var azkarCategoryAdapter: AzkarCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val azkarCategoryAzkarViewModel =
            ViewModelProvider(this).get(AzkarCategoryViewModel::class.java)

        binding = FragmentAzkarBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(true)


        azkarCategoryAdapter= AzkarCategoryAdapter(requireContext(), object : OnMyItemClickListener<AzkarCategory> {

            override fun onItemClick(itemObject: AzkarCategory, view: View?,position: Int) {
                val action: NavDirections = AzkarFragmentDirections.actionNavigationAzkarToElzekrFragment(itemObject.id,itemObject.catName,false)
                requireView().findNavController().navigate(action)
            }

            override fun onItemLongClick(itemObject: AzkarCategory, view: View?,position: Int) {
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
            requireView().findNavController().navigate(action)
        }

        binding.fabFavorite.setOnClickListener {
            val action: NavDirections = AzkarFragmentDirections.actionNavigationAzkarToElzekrFragment(0,"",true)
            requireView().findNavController().navigate(action)
        }



        return root
    }



}