package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.adapter.SoraAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentQuranBinding
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class QuranFragment : Fragment() {

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


        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= SoraAdapter(requireContext(), object : OnMyItemClickListener<Sora>{

            override fun onItemClick(itemObject: Sora, view: View?) {
                Toast.makeText(requireContext(),itemObject.name,Toast.LENGTH_LONG).show()
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



        return root
    }


}