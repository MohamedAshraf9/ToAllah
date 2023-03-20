package com.megahed.eqtarebmenalla.feature_data.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaHefzAdapter
import com.megahed.eqtarebmenalla.adapter.QuranImageAdapter
import com.megahed.eqtarebmenalla.databinding.FragmentQuranImageBinding
import com.megahed.eqtarebmenalla.databinding.FragmentSongBinding
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImageItem
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.QuranImageViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuranImageFragment : Fragment() , MenuProvider {
    private lateinit var binding: FragmentQuranImageBinding

    private val viewModel: QuranImageViewModel by viewModels()
    private var soraId:Int?=null
    private var soraName:String?=null
    private lateinit var quranImageAdapter : QuranImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soraId = arguments?.let { QuranImageFragmentArgs.fromBundle(it).soraId }
        soraName = arguments?.let { QuranImageFragmentArgs.fromBundle(it).soraName }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuranImageBinding.inflate(inflater, container, false)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title=soraName?:""
        toolbar.setBackgroundColor(ContextCompat.getColor(App.getInstance(), R.color.transparent))

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)


        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranImageAdapter= QuranImageAdapter(requireActivity())

        binding.recyclerView.adapter = quranImageAdapter

        soraId?.let {
            viewModel.getQuranImage(it)
            lifecycleScope.launch{
                viewModel.state.collect{

                    val hash=HashSet<String>()
                    it.quranImage.sortedBy { it.ayah }.forEach {
                        if (it.page!=null) {
                            hash.add(it.page)
                        }
                    }
                    quranImageAdapter.setData(hash.sorted())
                    if (it.isLoading){
                        binding.progressBar.visibility=View.VISIBLE
                    }
                    else{
                        binding.progressBar.visibility=View.GONE
                    }

                }
            }
        }

        return binding.root
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