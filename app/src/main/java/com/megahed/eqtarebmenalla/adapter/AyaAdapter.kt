package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.CompareDrawable.bytesEqualTo
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.AyaItemBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener

class AyaAdapter (private val context: Context,
                  private val onMyItemClickListener: OnItemWithFavClickListener<Aya>
) : RecyclerView.Adapter<AyaAdapter.MyHolder>() {

    private var listData= mutableListOf<Aya>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data:List<Aya>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AyaItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val ayaTitle=binding.ayaTitle
        val ayaNumber=binding.ayaNumber
        val fav=binding.fav
        val play=binding.play

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AyaItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val aya= listData[position]

        holder.ayaTitle.text=aya.text
        holder.ayaNumber.text="${aya.numberInSurah} | ${Constants.SORA_OF_QURAN[aya.soraId]}   اضغط للتفسير   "
        if (aya.isVaForte){
            holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
        }
        else{
            holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }





        holder.fav.setOnClickListener {//for favorite Item
            onMyItemClickListener.onItemFavClick(listData[position],it)

        }

        holder.play.setOnClickListener {
            onMyItemClickListener.onItemLongClick(listData[position],it,position+1)


        }

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it,position+1)


        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}