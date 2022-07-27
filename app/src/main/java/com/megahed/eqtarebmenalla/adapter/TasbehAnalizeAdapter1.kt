package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.AnaliticRowBinding
import com.megahed.eqtarebmenalla.databinding.TasbehAnalizeItemBinding
import com.megahed.eqtarebmenalla.db.customModel.HoursDate
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter

class TasbehAnalizeAdapter1 (private val context: Context
) : RecyclerView.Adapter<TasbehAnalizeAdapter1.MyHolder>() {

    private var listData= mutableListOf<HoursDate>()

    fun setData(data:List<HoursDate>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AnaliticRowBinding) : RecyclerView.ViewHolder(binding.root) {

        val percentPagesDone=binding.percentPagesDone
        val progressBar=binding.progressBar
        val tasbehName=binding.tasbehName

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AnaliticRowBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val tasbeh= listData[position]

        holder.tasbehName.text=tasbeh.date
        holder.progressBar
        holder.percentPagesDone.text="${tasbeh.count}"


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}