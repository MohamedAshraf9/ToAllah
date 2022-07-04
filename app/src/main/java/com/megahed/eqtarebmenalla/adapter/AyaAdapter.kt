package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.AyaItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraItemBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class AyaAdapter (private val context: Context,
                  private val onMyItemClickListener: OnMyItemClickListener<Aya>
) : RecyclerView.Adapter<AyaAdapter.MyHolder>() {

    private var listData= mutableListOf<Aya>()

    fun setData(data:List<Aya>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AyaItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraNameAr=binding.tvListFavSurahName
    /*    val soraNameEn=binding.soraNameEn
        val soraInfo=binding.soraInfo
        val soraNumber=binding.soraNumber*/
        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AyaItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val aya= listData[position]

        holder.soraNameAr.text=aya.text

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}