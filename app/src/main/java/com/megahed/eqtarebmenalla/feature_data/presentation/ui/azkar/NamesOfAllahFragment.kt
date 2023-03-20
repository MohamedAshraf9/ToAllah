package com.megahed.eqtarebmenalla.feature_data.presentation.ui.azkar

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.NamesOfAllaAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentNamesOfAllahBinding
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allaNames.NamesOfAllah


class NamesOfAllahFragment : Fragment() , MenuProvider {
    private lateinit var binding: FragmentNamesOfAllahBinding
    private lateinit var namesOfAllaAdapter: NamesOfAllaAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentNamesOfAllahBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = getString(R.string.namesOfAllah)
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.setHasFixedSize(true)

        namesOfAllaAdapter = NamesOfAllaAdapter(requireContext())
        binding.recyclerView.adapter = namesOfAllaAdapter

        val fileInString: String =
            App.getInstance().assets.open("names_of_allah.json").bufferedReader().use { it.readText() }
        val data= Gson().fromJson(fileInString, NamesOfAllah::class.java)
        namesOfAllaAdapter.setData(data.data)


        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)



        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return when (menuItem.itemId) {
            android.R.id.home -> {
                Navigation.findNavController(requireView()).popBackStack()
            }
            else -> false
        }
    }
}