package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.FragmentListenerHelperBinding
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.Ayah
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.Eya
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.Reway
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.RecitersVerse
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh.TasbehFragmentDirections
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.HefzVM
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.HefzViewModel
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.IslamicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListenerHelperFragment : Fragment() , MenuProvider {

    private lateinit var binding: FragmentListenerHelperBinding
    //private lateinit var hefzVM: HefzVM
    private lateinit var dialogBox: Dialog
    lateinit var  player : ExoPlayer
    var arrEytMP3 = arrayListOf<Eya>()
    var soraNumbers = arrayListOf<Int>()
    var readers =  mutableListOf<RecitersVerse>()
    lateinit var ayetAdapter : AyetAdapter
    var soraId:Int=0
    var startAya:Int=0
    var endAya:Int=0
    var reader:RecitersVerse?=null
    private val mainViewModel : HefzViewModel by activityViewModels()
    var job:Job?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListenerHelperBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toolbar: Toolbar = binding.toolbar.toolbar
        (activity as AppCompatActivity?)!!.setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.title=getString(R.string.listeningToSave)
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)




        binding.listOfRewat.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                binding.listSouraName.setText("")
                reader = parent.getItemAtPosition(position) as RecitersVerse
                binding.soraStartSpinner.isEnabled = true
            }





        binding.listSouraName.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                binding.nbAya.setText("")
                binding.nbEyaEnd.setText("")
                binding.start.isEnabled = false
                soraId=position+1
                val sora = parent.getItemAtPosition(position) as String
                val soraNumber= Constants.SORA_OF_QURAN_WITH_NB_EYA[sora]!!
                soraNumbers.clear()
                for (i in 1..soraNumber){
                    soraNumbers.add(i)
                }
                val adapter1= ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
                adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.nbAya.setAdapter(adapter1)

                val adapter2= ArrayAdapter(requireContext(), R.layout.list_item_spinner, soraNumbers)
                adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.nbEyaEnd.setAdapter(adapter2)

                binding.soraStartEditText.isEnabled = true
            }



        binding.nbAya.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                 startAya = parent.getItemAtPosition(position) as Int
                binding.soraStartEndText.isEnabled = true
            }



        binding.nbEyaEnd.onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                 endAya = parent.getItemAtPosition(position) as Int
                binding.start.isEnabled = true
            }





        binding.stop.setOnClickListener {
            binding.start.visibility = View.VISIBLE
            binding.stop.visibility = View.GONE

            player.stop()
        }




        binding.start.setOnClickListener {
            if (MethodHelper.isOnline(requireContext())){
                reader?.let {

                    val link = if (it.audio_url_bit_rate_32_.trim()
                            .isNotEmpty() && it.audio_url_bit_rate_32_.trim() != "0"
                    ) {
                        it.audio_url_bit_rate_32_
                    } else if (it.audio_url_bit_rate_64.trim()
                            .isNotEmpty() && it.audio_url_bit_rate_64.trim() != "0"
                    ) {
                        it.audio_url_bit_rate_64
                    } else {
                        it.audio_url_bit_rate_128
                    }

                    val ayaR=binding.nbAyaRepeat.text.toString()
                    val soraR=binding.suraRepeat.text.toString()
                    if (ayaR.trim().isEmpty()|| soraR.trim().isEmpty()){
                        MethodHelper.toastMessage(getString(R.string.addValidData))
                    }else{
                        if (startAya>endAya){
                            MethodHelper.toastMessage(getString(R.string.ayaWrong))
                        }
                        else{
                            val action: NavDirections = ListenerHelperFragmentDirections
                                .actionListenerHelperFragmentToHefzRepeatFragment(
                                    link,soraId.toString(),startAya.toString(),endAya.toString(),
                                    ayaR.toInt(),
                                    soraR.toInt(),
                                    reader?.name!!)
                            Navigation.findNavController(requireView()).navigate(action)
                        }

                    }




                }



//            hefzVM.getSuraMp3(tempSuraId ,rewayIdSelected).observe(viewLifecycleOwner, Observer {
//
//                this.dialogBox = Dialog(requireContext(),android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
//                this.dialogBox.setContentView(R.layout.ayet_alert)
//                val window: Window = dialogBox.getWindow()!!
//                val wlp = window.attributes
//                wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
//                window.attributes = wlp
//                val alert : ConstraintLayout = this.dialogBox.findViewById(R.id.alert_constraint)
//                //alert.setBackgroundColor(Color.parseColor("#ffffff"))
//                val btnStop: TextView = this.dialogBox.findViewById(R.id.stop)
//
//                 ayetAdapter = AyetAdapter(this.requireContext(), arrEytMP3)
//                val rcAyet: RecyclerView = this.dialogBox.findViewById(R.id.re_ayet)
//                rcAyet.layoutManager = LinearLayoutManager(this.requireContext())
//                rcAyet.adapter = ayetAdapter
//
//
//                dialogBox.show()
//                binding.start.visibility = View.GONE
//                binding.stop.visibility = View.VISIBLE
//                btnStop.setOnClickListener {
//                    player.stop()
//                    dialogBox.cancel()
//                    binding.start.visibility = View.VISIBLE
//                    binding.stop.visibility = View.GONE
//                }
//
////                rcAyet.adapter = ayetAdapter
//              ///  for(i in 0..binding.suraRepeat.text.toString().toString().toInt()-1){
//                    soundRepeat(it.data.ayahs,
//                        binding.nbAya.text.toString().toInt()-1,
//                        binding.nbEyaEnd.text.toString().toInt()-1,
//                        binding.nbAyaRepeat.text.toString().toInt(),
//                        binding.suraRepeat.text.toString().toString().toInt()-1,)
//            //    }
//
//            })


            }
            else MethodHelper.toastMessage(getString(R.string.checkConnection))
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


    override fun onStart() {
        super.onStart()
        job= lifecycleScope.launch {
            mainViewModel.state.collect { ayaHefzState ->
                val filter=ayaHefzState.recitersVerse.filter {
                    (it.audio_url_bit_rate_32_.trim().isNotEmpty() && !it.audio_url_bit_rate_32_.trim().equals("0"))||
                            (it.audio_url_bit_rate_64.trim().isNotEmpty() && !it.audio_url_bit_rate_64.trim().equals("0"))||
                            (it.audio_url_bit_rate_128.trim().isNotEmpty() && !it.audio_url_bit_rate_128.trim().equals("0"))
                }
                readers.addAll(filter)
                val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, readers)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.listOfRewat.setAdapter(adapter)

            }
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_spinner, Constants.SORA_OF_QURAN_WITH_NB_EYA.map { it.key })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.listSouraName.setAdapter(adapter)
    }



}