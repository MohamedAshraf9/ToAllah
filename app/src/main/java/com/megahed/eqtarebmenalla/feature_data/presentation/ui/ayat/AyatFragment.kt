package com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat

import android.net.Uri
import android.os.Bundle
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
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.adapter.AyaAdapter
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.FragmentAyatBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.states.NewTaskSheet
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AyatFragment : Fragment(),MenuProvider {
    private lateinit var binding: FragmentAyatBinding
    private lateinit var quranTextAdapter : AyaAdapter
    private var soraId:Int?=null
    private var soraName:String?=null
    private var fromFavorite:Boolean=false

    lateinit var  player : ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soraId = arguments?.let { AyatFragmentArgs.fromBundle(it).soraId }
        soraName = arguments?.let { AyatFragmentArgs.fromBundle(it).soraName }
        fromFavorite = arguments?.let { AyatFragmentArgs.fromBundle(it).fromFavorite }!!
        player = ExoPlayer.Builder(requireContext()).build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ayaViewModel =
            ViewModelProvider(this).get(AyaViewModel::class.java)

        binding = FragmentAyatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)



        val verticalLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = verticalLayoutManager
        binding.recyclerView.setHasFixedSize(true)

        quranTextAdapter= AyaAdapter(requireContext(), object : OnItemWithFavClickListener<Aya> {

            override fun onItemFavClick(itemObject: Aya, view: View?) {
                //fav icon click
                itemObject.isVaForte=!itemObject.isVaForte
                ayaViewModel.updateAya(itemObject)
            }

            override fun onItemClick(itemObject: Aya, view: View?, position: Int) {
                val sheet= NewTaskSheet(itemObject,itemObject.numberInSurah.toString())
                sheet.show(requireActivity().supportFragmentManager,"sheet")
            }

            override fun onItemLongClick(itemObject: Aya, view: View?,position: Int) {
                //play Aya
                if (MethodHelper.isOnline(requireContext())) {
                    player.stop()
                    player.removeMediaItem(0)
                    MethodHelper.toastMessage(getString(R.string.loading))
                    val audioItem: MediaItem = MediaItem.fromUri(
                        Uri.parse(
                            "https://verse.mp3quran.net/arabic/mohammad_alminshawi/64/" + CommonUtils.convertSora(
                                itemObject.soraId.toString(),
                                itemObject.numberInSurah.toString()
                            ) + ".mp3"
                        )
                    )
                    player.addMediaItem(audioItem)
                    player.prepare()
                    player.play()
                }
                else MethodHelper.toastMessage(getString(R.string.checkConnection))
            }
        })
        binding.recyclerView.adapter = quranTextAdapter
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (!fromFavorite){
            toolbar.title = soraName
            lifecycleScope.launchWhenStarted {

                soraId?.let { it ->
                    ayaViewModel.getAyaOfSoraId(it).collect{ayatList ->
                        if (ayatList.isEmpty()) {
                            binding.loadingContainer.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.lottieView.visibility = View.VISIBLE
                            binding.loadingText.visibility = View.GONE
                        } else {
                            binding.loadingContainer.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.lottieView.visibility = View.GONE
                            binding.loadingText.visibility = View.GONE
                        }
                        quranTextAdapter.setData(ayatList)

                    }
                }
            }


        }else{
            toolbar.title = getString(R.string.favorite)

            lifecycleScope.launchWhenStarted {

                    ayaViewModel.getFavoriteAya().collect{ayatFavList->
                        if (ayatFavList.isEmpty()) {
                            binding.loadingContainer.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            binding.lottieView.visibility = View.VISIBLE
                            binding.loadingText.visibility = View.VISIBLE
                        } else {
                            binding.loadingContainer.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.lottieView.visibility = View.GONE
                            binding.loadingText.visibility = View.GONE
                        }
                        quranTextAdapter.setData(ayatFavList)

                    }

            }


        }








        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

        return  when (menuItem.itemId) {
            android.R.id.home -> {
                player.stop()
                 Navigation.findNavController(requireView()).popBackStack()
            }
            else -> false
        }
    }
}