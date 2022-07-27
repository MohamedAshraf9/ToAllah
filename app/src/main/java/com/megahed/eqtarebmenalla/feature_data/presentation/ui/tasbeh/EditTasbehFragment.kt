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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.EditTasbehAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils.showMessage
import com.megahed.eqtarebmenalla.databinding.FragmentEditTasbehBinding
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.myListener.OnTasbehEditListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTasbehFragment : Fragment(), MenuProvider {


    private lateinit var binding: FragmentEditTasbehBinding
    private lateinit var tasbehViewModel :TasbehViewModel
    private lateinit var editTasbehAdapter: EditTasbehAdapter


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

        toolbar.title=getString(R.string.editTasbeh)

        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        editTasbehAdapter= EditTasbehAdapter(requireContext(), object : OnTasbehEditListener<Tasbeh> {
            override fun onUpdateClick(newText: String, itemObject: Tasbeh, view: View?) {
                if (newText.trim().isNotBlank()){
                    itemObject.tasbehName=newText
                    tasbehViewModel.updateTasbeh(itemObject)
                    showMessage(requireContext(),getString(R.string.updated))
                }
                else{
                    showMessage(requireContext(),getString(R.string.addValidData))
                }
            }

            override fun onDeleteClick(itemObject: Tasbeh, view: View?) {
                deleteTasbeh(itemObject)

            }


        })

        binding.recyclerView.adapter = editTasbehAdapter

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        lifecycleScope.launchWhenStarted {
            tasbehViewModel.getAllTasbeh().collect{
                editTasbehAdapter.setData(it)
            }
        }



        return root
    }

    private fun deleteTasbeh(itemObject: Tasbeh) {
        val d = resources.getDrawable(R.drawable.dialog_bg,requireContext().theme)
        MaterialAlertDialogBuilder(requireContext(),
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.deleteConferm))
            .setNegativeButton(resources.getString(R.string.cancle)) { dialog, which ->
                // Respond to negative button press
                dialog.cancel()
            }
            .setPositiveButton(resources.getString(R.string.delete)) { dialog, which ->
                // Respond to positive button press
                tasbehViewModel.deleteTasbeh(itemObject)

            }.setBackground(d)
            .show()
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