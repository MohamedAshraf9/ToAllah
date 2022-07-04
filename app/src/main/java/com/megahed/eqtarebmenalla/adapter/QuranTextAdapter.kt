package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.SoraItemBinding
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class QuranTextAdapter (private val context: Context,
private val onMyItemClickListener: OnMyItemClickListener<Sora>
) : RecyclerView.Adapter<QuranTextAdapter.MyHolder>() {

    private var listData= mutableListOf<Sora>()

    fun setData(data:List<Sora>){
        listData.clear()
        listData.addAll(data)
    }


    class MyHolder(binding: SoraItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraNameAr=binding.soraNameAr
        val soraNameEn=binding.soraNameEn
        val soraInfo=binding.soraInfo
        val soraNumber=binding.soraNumber
        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(SoraItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}