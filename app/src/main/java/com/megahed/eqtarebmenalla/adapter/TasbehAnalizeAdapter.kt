package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.TasbehAnalizeItemBinding
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter

class TasbehAnalizeAdapter (private val context: Context
) : RecyclerView.Adapter<TasbehAnalizeAdapter.MyHolder>() {

    private var listData= mutableListOf<TasbehCounter>()

    fun setData(data:List<TasbehCounter>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: TasbehAnalizeItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val tasbehName=binding.tasbehName
        val tasbehNumber=binding.tasbehNumber

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(TasbehAnalizeItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val tasbeh= listData[position]

        holder.tasbehName.text=tasbeh.tasbehName
        holder.tasbehNumber.text="${tasbeh.count}"


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}