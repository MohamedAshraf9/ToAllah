package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.AyaItemBinding
import com.megahed.eqtarebmenalla.databinding.AzkarCategoryItemBinding
import com.megahed.eqtarebmenalla.databinding.ElzekrItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraItemBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class ElzekrAdapter (private val context: Context,
                     private val onMyItemClickListener: OnMyItemClickListener<ElZekr>
) : RecyclerView.Adapter<ElzekrAdapter.MyHolder>() {

    private var listData= mutableListOf<ElZekr>()

    fun setData(data:List<ElZekr>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: ElzekrItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val zekr=binding.zekr
        val description=binding.description
        val referenceAndCount=binding.referenceAndCount
        val fav=binding.fav



        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(ElzekrItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val zakarCat= listData[position]

        holder.zekr.text=zakarCat.zekr


        if (zakarCat.isVaForte){
            holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
        }
        else{
            holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }


        holder.fav.setOnClickListener {//for favorite Item
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}