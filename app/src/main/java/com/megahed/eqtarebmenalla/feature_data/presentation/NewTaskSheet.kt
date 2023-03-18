package com.megahed.eqtarebmenalla.feature_data.presentation


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.databinding.TafserSheetBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.AllQuran
import com.megahed.eqtarebmenalla.feature_data.data.tafser.Tafser
import com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels.TafsirVM

class NewTaskSheet(private val data: Aya,private val ayaNumber:String) : BottomSheetDialogFragment()
{

    private lateinit var binding: TafserSheetBinding




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TafserSheetBinding.inflate(inflater,container,false)

        val fileInString: String =
            App.getInstance().assets.open("tafseer.json").bufferedReader().use { it.readText() }
        val data1= Gson().fromJson(fileInString, Tafser::class.java)
       val result= data1.filter { it.number==data.soraId.toString()&&it.aya==ayaNumber }
        result.get(0).let {
            binding.tafsir.text = it.text
            binding.aya.text = data.text
        }
       /* tafsirVM.getTafsir(data.soraId.toString(), data.ayaId.toString()).observe(this, Observer {
            it?.let {
                binding.tafsir.text = it.text
                binding.aya.text = data.text
            }

        })
*/

        return binding.root
    }






}