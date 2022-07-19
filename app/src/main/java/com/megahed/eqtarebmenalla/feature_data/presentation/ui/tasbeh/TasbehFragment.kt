package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.FragmentTasbehBinding
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TasbehFragment : Fragment() {

    private lateinit var binding: FragmentTasbehBinding
    private var listData= mutableListOf<Tasbeh>()
    private var counter:Int=0
    private var tasbehCounter:Tasbeh?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasbehBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val tasbehViewModel =
            ViewModelProvider(this).get(TasbehViewModel::class.java)

        val unitsArrayAdapter: ArrayAdapter<Tasbeh> =
            ArrayAdapter<Tasbeh>(requireContext(), R.layout.list_item_spinner, listData)
        unitsArrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        (binding.tasbehSpinner.editText as MaterialAutoCompleteTextView).setAdapter(unitsArrayAdapter)


        lifecycleScope.launchWhenStarted {

            tasbehViewModel.getAllTasbeh().collect{
                listData.clear()
                listData.addAll(it)
            }
        }

        (binding.tasbehSpinner.editText as MaterialAutoCompleteTextView).onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                tasbehCounter = parent.getItemAtPosition(position) as Tasbeh

            }


        binding.tasbehCounter.increaseButton.setOnClickListener {
            counter+=10000
            if (counter>=100000)
                binding.tasbehCounter.textCountar.textSize=38f
            binding.tasbehCounter.textCountar.text="$counter"
            tasbehCounter?.let {

            }

        }
        binding.tasbehCounter.resetButton.setOnClickListener {
            counter=0
            binding.tasbehCounter.textCountar.text="$counter"
        }

        return root
    }

}