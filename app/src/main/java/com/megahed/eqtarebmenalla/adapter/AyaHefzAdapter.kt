package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.AyaItemBinding
import com.megahed.eqtarebmenalla.databinding.AyaItemMp3Binding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.tafsir.TafsirActivity
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class AyaHefzAdapter (private val context: Context
) : RecyclerView.Adapter<AyaHefzAdapter.MyHolder>() {

    private var listData= mutableListOf<Aya>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data:List<Aya>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AyaItemMp3Binding) : RecyclerView.ViewHolder(binding.root) {

        val ayaTitle=binding.aya
        val ayaNumber=binding.ayaId

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AyaItemMp3Binding.inflate(LayoutInflater.from(context), parent, false))


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val aya= listData[position]

        holder.ayaTitle.text=aya.text
        holder.ayaNumber.text="${aya.numberInSurah}"



    }

    override fun getItemCount(): Int {
        return listData.size
    }


}