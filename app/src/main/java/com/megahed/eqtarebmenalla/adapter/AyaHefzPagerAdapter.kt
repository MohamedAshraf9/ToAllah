package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.AyaItemMp3Binding
import com.megahed.eqtarebmenalla.db.model.Aya

class AyaHefzPagerAdapter(private val context: Context) : RecyclerView.Adapter<AyaHefzPagerAdapter.MyHolder>() {

    private var listData = mutableListOf<Aya>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Aya>) {
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }

    class MyHolder(binding: AyaItemMp3Binding) : RecyclerView.ViewHolder(binding.root) {
        val ayaTitle = binding.aya
        val ayaNumber = binding.ayaId
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(AyaItemMp3Binding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val aya = listData[position]
        holder.ayaTitle.text = aya.text
        holder.ayaNumber.text = "${aya.numberInSurah}"

        val layoutParams = holder.root.layoutParams
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        holder.root.layoutParams = layoutParams
    }

    override fun getItemCount(): Int {
        return listData.size
    }
}