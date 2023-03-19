package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.SoraItemBinding
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.util.*

class SoraAdapter (private val context: Context,
                   private val onMyItemClickListener: OnMyItemClickListener<Sora>
) : RecyclerView.Adapter<SoraAdapter.MyHolder>(), Filterable {

    private var listData= mutableListOf<Sora>()
    private var listDataSearch= mutableListOf<Sora>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data:List<Sora>){
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: SoraItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraNameAr=binding.soraNameAr

        val soraInfo=binding.soraInfo
        val soraNumber=binding.soraNumber
        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(SoraItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val sora= listData[position]

        holder.soraNameAr.text=sora.name

        val ayat=App.getInstance().getString(R.string.ayat)
        var revelationType=sora.revelationType
        revelationType = if (revelationType == "Meccan"){
            "مكيه"
        }else{
            "مدنيه"
        }

        val s="$revelationType - ${sora.ayatNumbers} $ayat"
        holder.soraInfo.text=s
        holder.soraNumber.text="${sora.soraId}"

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it,0)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }

    private val examplefilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterlist = mutableListOf<Sora>()
            if (charSequence.isEmpty()) {
                filterlist.addAll(listDataSearch)
            } else {
                val filterPattern =
                    charSequence.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (item in listDataSearch) {
                    if (item.name.lowercase().contains(filterPattern)) {
                        filterlist.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.values = filterlist
            return results
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            if (filterResults.values != null) {
                listData.clear()
                listData.addAll(filterResults.values as MutableList<Sora>)
                notifyDataSetChanged()
            }
        }
    }

    override fun getFilter(): Filter {

        return examplefilter

    }


}