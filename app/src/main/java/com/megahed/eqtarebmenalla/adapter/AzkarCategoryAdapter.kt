package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.AyaItemBinding
import com.megahed.eqtarebmenalla.databinding.AzkarCategoryItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraItemBinding
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class AzkarCategoryAdapter (private val context: Context,
                            private val onMyItemClickListener: OnMyItemClickListener<AzkarCategory>
) : RecyclerView.Adapter<AzkarCategoryAdapter.MyHolder>() {

    private var listData= mutableListOf<AzkarCategory>()

    fun setData(data:List<AzkarCategory>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: AzkarCategoryItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val zakarCatName=binding.zakarCatName


        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AzkarCategoryItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val zakarCat= listData[position]

        holder.zakarCatName.text=zakarCat.catName

        holder.itemView.setOnClickListener {//for favorite Item
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}