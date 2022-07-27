package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

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
import com.megahed.eqtarebmenalla.adapter.EditTasbehAdapter
import com.megahed.eqtarebmenalla.adapter.TasbehAnalizeAdapter
import com.megahed.eqtarebmenalla.adapter.TasbehAnalizeAdapter1
import com.megahed.eqtarebmenalla.adapter.TasbehAnalizeAdapter2
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.FragmentEditTasbehBinding
import com.megahed.eqtarebmenalla.databinding.FragmentTasbehAnalizeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class TasbehAnalizeFragment : Fragment(), MenuProvider {


    private lateinit var binding: FragmentTasbehAnalizeBinding
    private lateinit var tasbehViewModel :TasbehViewModel
    private lateinit var tasbehAnalizeAdapter: TasbehAnalizeAdapter
    private lateinit var tasbehAnalizeAdapter1: TasbehAnalizeAdapter1
    private lateinit var tasbehAnalizeAdapter2: TasbehAnalizeAdapter2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasbehAnalizeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        tasbehViewModel =
            ViewModelProvider(this).get(TasbehViewModel::class.java)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title=getString(R.string.analyze)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)


        //recycler 1
        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        tasbehAnalizeAdapter= TasbehAnalizeAdapter(requireContext())
        binding.recyclerView.adapter = tasbehAnalizeAdapter

        //recycler 2
        val verticalLayoutManager1 =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerAnalyticMonth.layoutManager = verticalLayoutManager1
        binding.recyclerAnalyticMonth.setHasFixedSize(true)

        tasbehAnalizeAdapter1= TasbehAnalizeAdapter1(requireContext())
        binding.recyclerAnalyticMonth.adapter = tasbehAnalizeAdapter1


        //recycler 3
        val verticalLayoutManager2 =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerAnalyticTasbeh.layoutManager = verticalLayoutManager2
        binding.recyclerAnalyticTasbeh.setHasFixedSize(true)

        tasbehAnalizeAdapter2= TasbehAnalizeAdapter2(requireContext())
        binding.recyclerAnalyticTasbeh.adapter = tasbehAnalizeAdapter2


        lifecycleScope.launchWhenStarted {
            tasbehViewModel.getTasbehCounter().collect{
                tasbehAnalizeAdapter.setData(it)
                tasbehAnalizeAdapter2.setData(it,it.sumOf { it.count })
                binding.bestTasbehCounter.text="${it.maxByOrNull{ it.count }?.count ?: getString(R.string.no_analysis)}"
                binding.bestTasbeh.text= it.maxByOrNull{ it.count }?.tasbehName ?: getString(R.string.no_analysis)
                binding.totalTasbehNumber.text="${it.sumOf { it.count }}"

            }
        }

        lifecycleScope.launchWhenStarted {
            tasbehViewModel.getBestDays().collect{
               binding.dayBestCounter.text="${it.maxByOrNull{ it.count }?.count ?: getString(R.string.no_analysis)}"
                val textDate= it.maxByOrNull{ it.date }?.date?.let { it1 -> CommonUtils.getDay(it1) }
               binding.dayBest.text= textDate ?: getString(R.string.no_analysis)
                binding.daysNumber.text="${it.size}"
            }
        }

        lifecycleScope.launchWhenStarted {
            tasbehViewModel.getDataOfMonths().collect{
                tasbehAnalizeAdapter1.setData(it,it.sumOf { it.count })
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