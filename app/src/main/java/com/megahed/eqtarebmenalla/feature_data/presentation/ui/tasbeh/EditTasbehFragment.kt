package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.EditTasbehAdapter
import com.megahed.eqtarebmenalla.adapter.ElzekrAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentEditTasbehBinding
import com.megahed.eqtarebmenalla.databinding.FragmentTasbehBinding
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import com.megahed.eqtarebmenalla.myListener.OnTasbehEditListener

class EditTasbehFragment : Fragment() {


    private lateinit var binding: FragmentEditTasbehBinding
    private lateinit var tasbehViewModel :TasbehViewModel
    private lateinit var editTasbehAdapter: EditTasbehAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditTasbehBinding.inflate(inflater, container, false)
        val root: View = binding.root

        tasbehViewModel =
            ViewModelProvider(this).get(TasbehViewModel::class.java)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        editTasbehAdapter= EditTasbehAdapter(requireContext(), object : OnTasbehEditListener<Tasbeh> {
            override fun onUpdateClick(newText: String, itemObject: Tasbeh, view: View?) {
                itemObject.tasbehName=newText
                tasbehViewModel.updateTasbeh(itemObject)
            }

            override fun onDeleteClick(itemObject: Tasbeh, view: View?) {
                //todo show message before delete
                tasbehViewModel.deleteTasbeh(itemObject)
            }


        })

        binding.recyclerView.adapter = editTasbehAdapter

        lifecycleScope.launchWhenStarted {
            tasbehViewModel.getAllTasbeh().collect{
                editTasbehAdapter.setData(it)
            }
        }


        return root
    }

}