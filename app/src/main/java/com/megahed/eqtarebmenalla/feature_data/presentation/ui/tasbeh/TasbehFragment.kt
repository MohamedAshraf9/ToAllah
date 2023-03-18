package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.FragmentTasbehBinding
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehData
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class TasbehFragment : Fragment() {

    private lateinit var binding: FragmentTasbehBinding
    private lateinit var tasbehViewModel :TasbehViewModel
    private var listData= mutableListOf<Tasbeh>()
    private var counter:Int=0
    private var tasbehCounter:Tasbeh?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasbehBinding.inflate(inflater, container, false)
        val root: View = binding.root

         tasbehViewModel =
            ViewModelProvider(this).get(TasbehViewModel::class.java)

        val unitsArrayAdapter: ArrayAdapter<Tasbeh> =
            ArrayAdapter<Tasbeh>(requireContext(), R.layout.list_item_spinner, listData)
        unitsArrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        (binding.tasbehSpinner.editText as MaterialAutoCompleteTextView).setAdapter(unitsArrayAdapter)


        lifecycleScope.launchWhenStarted {

            tasbehViewModel.getAllTasbeh().collect{
                listData.clear()
                listData.addAll(it)
            }
        }

        val str = Calendar.getInstance()
        str.set(Calendar.HOUR_OF_DAY, 0)
        str.set(Calendar.MINUTE, 0)
        str.set(Calendar.SECOND, 0)

        val end = Calendar.getInstance()
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)

        (binding.tasbehSpinner.editText as MaterialAutoCompleteTextView).onItemClickListener =
            OnItemClickListener { parent, view, position, id ->
                tasbehCounter = parent.getItemAtPosition(position) as Tasbeh
                tasbehCounter?.let {tasbeh ->
                    lifecycleScope.launchWhenStarted {
                        tasbehViewModel.getTasbehDataToday(tasbeh.id,str.time,end.time)?.let {
                                counter=it.target
                                binding.tasbehCounter.textCountar.text="${it.target}"

                        }?:run {
                            counter=0
                                val now = Calendar.getInstance()
                                val tasbehData = TasbehData(now.time, counter, tasbeh.id)
                                tasbehViewModel.insertTasbehData(tasbehData)
                                //Log.d("MyLogD", "no")
                                /*withContext(Dispatchers.Main){
                                    Toast.makeText(requireContext(),"Hi No",Toast.LENGTH_LONG).show()
                                }*/
                            binding.tasbehCounter.textCountar.text="$counter"
                            }


                    }

                }

            }


        binding.tasbehCounter.increaseButton.setOnClickListener {
            if(tasbehCounter==null){
                if (counter==0)
                    showDialog()
                binding.tasbehCounter.textCountar.text="$counter"
            }

            ++counter
            if (counter>=100000)
                binding.tasbehCounter.textCountar.textSize=36f

            tasbehCounter?.let {tasbeh ->
                lifecycleScope.launchWhenStarted {
                    tasbehViewModel.getTasbehDataToday(tasbeh.id,str.time,end.time)?.let {
                        it.target+=1
                        tasbehViewModel.updateTasbehData(it)
                        binding.tasbehCounter.textCountar.text="${it.target}"

                        if (counter>=100000)
                            binding.tasbehCounter.textCountar.textSize=36f

                    }?:run {
                        val now = Calendar.getInstance()
                        val tasbehData = TasbehData(now.time, counter, tasbeh.id)
                        tasbehViewModel.insertTasbehData(tasbehData)
                        binding.tasbehCounter.textCountar.text="$counter"


                    }
                }

            }


        }
        binding.tasbehCounter.resetButton.setOnClickListener {
            counter=0
            binding.tasbehCounter.textCountar.text="$counter"
        }

        binding.fabAddTasbeh.setOnClickListener {


            val d = resources.getDrawable(R.drawable.dialog_bg,requireContext().theme)
            val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.add_tasbeh, null)
            val addTasbehText=view.findViewById<TextInputLayout>(R.id.addTasbehText)
            MaterialAlertDialogBuilder(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                .setMessage(R.string.addTasbeh)
                .setView(view)
                .setNegativeButton(resources.getString(R.string.cancle)) { dialog, which ->
                    // Respond to negative button press
                    dialog.cancel()
                }
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                    // Respond to positive button press
                    tasbehViewModel.insertTasbeh(
                        Tasbeh(addTasbehText.editText?.text.toString())
                    )

                }.setBackground(d)
                .show()

        }
        binding.fabEditTasbeh.setOnClickListener {
            val action: NavDirections = TasbehFragmentDirections.actionNavigationTasbehToEditTasbehFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }


        binding.fabAnalyze.setOnClickListener {
            val action: NavDirections = TasbehFragmentDirections.actionNavigationTasbehToTasbehAnalizeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }




        return root
    }

    private fun showDialog() {
        val d = resources.getDrawable(R.drawable.dialog_bg,requireContext().theme)
        MaterialAlertDialogBuilder(requireContext(),
            com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
            .setTitle(getString(R.string.alert))
            .setMessage(getString(R.string.alertDesc))
            .setNegativeButton(resources.getString(R.string.ok)) { dialog, which ->
                // Respond to negative button press
                dialog.cancel()
            }.setBackground(d)
            .show()
    }

    private fun getCounterData(str: Calendar, end: Calendar,show:Boolean) {
        tasbehCounter?.let {tasbeh ->
            lifecycleScope.launchWhenStarted {
                tasbehViewModel.getTasbehDataToday(tasbeh.id,str.time,end.time)?.let {
                    if (!show){
                        it.target+=1
                        tasbehViewModel.updateTasbehData(it)
                    }
                    else{
                        counter=it.target
                        binding.tasbehCounter.textCountar.text="${it.target}"
                    }

                }?:run {
                    if (!show) {
                        val now = Calendar.getInstance()
                        val tasbehData = TasbehData(now.time, counter, tasbeh.id)
                        tasbehViewModel.insertTasbehData(tasbehData)
                        //Log.d("MyLogD", "no")
                        /*withContext(Dispatchers.Main){
                            Toast.makeText(requireContext(),"Hi No",Toast.LENGTH_LONG).show()
                        }*/
                    }
                    binding.tasbehCounter.textCountar.text="$counter"
                }
            }

        }
    }

}