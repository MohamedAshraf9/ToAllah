package com.megahed.eqtarebmenalla.feature_data.presentation.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.megahed.eqtarebmenalla.adapter.AyaHefzAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.FragmentHefzRepeatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat.AyaViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HefzRepeatFragment : Fragment(), MenuProvider {

    private lateinit var binding: FragmentHefzRepeatBinding
    private var link:String?=null
    private var soraId:String?=null
    private var startAya:String?=null
    private var readerName:String?=null
    private var endAya:String?=null
    private var ayaRepeat:Int?=null
    private var allRepeat:Int?=null
    private lateinit var quranTextAdapter : AyaHefzAdapter

    lateinit var  player : ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        link = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).link }
        soraId = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).soraId }
        startAya = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).startAya }
        endAya = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).endAya }
        ayaRepeat = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).ayaRepeat }
        allRepeat = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).allRepeat }
        readerName = arguments?.let { HefzRepeatFragmentArgs.fromBundle(it).readerName }


        player = ExoPlayer.Builder(requireContext()).build()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHefzRepeatBinding.inflate(inflater, container, false)

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= AyaHefzAdapter(requireContext())

        binding.recyclerView.adapter = quranTextAdapter


        val ayaViewModel =
            ViewModelProvider(this).get(AyaViewModel::class.java)

        soraId?.let { it ->
        lifecycleScope.launchWhenStarted {



                ayaViewModel.getAyaOfSoraId(it.toInt()).collect { it1 ->
                    val ayaList = mutableListOf<Aya>()
                    for (i in startAya?.toInt()!! - 1 until endAya?.toInt()!!) {
                        ayaList.add(it1[i])
                    }
                    quranTextAdapter.setData(ayaList)


                }


            }
            for (k in 0 until allRepeat!! ) {
                for (i in 0 until endAya?.toInt()!!) {
                    for (j in 0 until ayaRepeat!!) {
                        val audioItem: MediaItem = MediaItem.fromUri(Uri.parse(link!! + CommonUtils.convertSora(
                            soraId!!,
                            (startAya!!.toInt()+i).toString()
                        ) + ".mp3"
                        ))
                        Log.d("sdsdsd",link!! + CommonUtils.convertSora(
                            soraId!!,
                            (startAya!!.toInt()+i).toString()
                        ) + ".mp3")


                        player.addMediaItem(audioItem)
                            player.prepare()
                            player.play()

                    }
                }
            }
//            player.prepare()
//            player.play()


        }

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)


        return  binding.root
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