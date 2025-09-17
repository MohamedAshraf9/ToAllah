package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.common.CommonUtils.normalizeArabic
import com.megahed.eqtarebmenalla.databinding.AzkarCategoryItemBinding
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener

class AzkarCategoryAdapter(
    private val context: Context,
    private val onMyItemClickListener: OnMyItemClickListener<AzkarCategory>
) : RecyclerView.Adapter<AzkarCategoryAdapter.MyHolder>() {

    private var listData = mutableListOf<AzkarCategory>()
    private var filteredList = mutableListOf<AzkarCategory>()

    fun setData(data: List<AzkarCategory>) {
        listData.clear()
        listData.addAll(data)
        filteredList.clear()
        filteredList.addAll(data)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(listData)
        } else {
            for (item in listData) {
                if (item.catName.contains(query, ignoreCase = true)) {
                    filteredList.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    class MyHolder(binding: AzkarCategoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val zakarCatName = binding.zakarCatName
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(AzkarCategoryItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val zakarCat = filteredList[position]
        holder.zakarCatName.text = zakarCat.catName.normalizeArabic()

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(filteredList[position], it, position)
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }
}