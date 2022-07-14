package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.AllahNamesItemBinding
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allaNames.Data

class NamesOfAllaAdapter (private val context: Context
) : RecyclerView.Adapter<NamesOfAllaAdapter.MyHolder>() {

    private var listData= mutableListOf<Data>()

    fun setData(data:List<Data>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AllahNamesItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val name=binding.name

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AllahNamesItemBinding.inflate(LayoutInflater.from(context), parent, false))

    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val names= listData[position]
        holder.name.text=names.name



    }

    override fun getItemCount(): Int {
        return listData.size
    }


}