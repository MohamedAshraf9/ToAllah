package com.megahed.eqtarebmenalla.feature_data.presentation.ui.elzekr

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
import com.megahed.eqtarebmenalla.adapter.ElzekrAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentElzekrBinding
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyatFragmentArgs
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class ElzekrFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentElzekrBinding
    private lateinit var elzekrAdapter: ElzekrAdapter
    private var azkarCatId:Int?=null
    private var azkarName:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        azkarCatId = arguments?.let { ElzekrFragmentArgs.fromBundle(it).azkarCatId }
        azkarName = arguments?.let { ElzekrFragmentArgs.fromBundle(it).azkarName }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val elzekrViewModel =
            ViewModelProvider(this).get(ElzekrViewModel::class.java)

        binding = FragmentElzekrBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title = azkarName

        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        elzekrAdapter= ElzekrAdapter(requireContext(), object : OnMyItemClickListener<ElZekr> {

            override fun onItemClick(itemObject: ElZekr, view: View?) {
                //fav icon click
                itemObject.isVaForte=!itemObject.isVaForte
                elzekrViewModel.updateElZekr(itemObject)
            }

            override fun onItemLongClick(itemObject: ElZekr, view: View?) {
            }
        })
        binding.recyclerView.adapter = elzekrAdapter
        lifecycleScope.launchWhenStarted {

            azkarCatId?.let { it ->
                elzekrViewModel.getElZekrOfCatId(it).collect{ it1 ->
                    elzekrAdapter.setData(it1)

                }
            }
        }


        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)



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