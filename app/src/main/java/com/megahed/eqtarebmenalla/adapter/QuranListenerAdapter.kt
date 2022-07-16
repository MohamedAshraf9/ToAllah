package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.QuranListenerItemBinding
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.util.*

class QuranListenerAdapter (private val context: Context,
                            private val onMyItemClickListener: OnMyItemClickListener<Reciter>
) : RecyclerView.Adapter<QuranListenerAdapter.MyHolder>(), Filterable {

    private var listData= mutableListOf<Reciter>()
    private var listDataSearch= mutableListOf<Reciter>()

    fun setData(data:List<Reciter>){
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: QuranListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val count=binding.count
        val letter=binding.letter
        val readerName=binding.readerName
        val rewayaInfo=binding.rewayaInfo

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(QuranListenerItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val quranListener= listData[position]

        holder.readerName.text=quranListener.name
        holder.letter.text=quranListener.letter
        val s= App.getInstance().getString(R.string.sora)
        holder.count.text="${quranListener.count} $s"
        holder.rewayaInfo.text=quranListener.rewaya



        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


    private val examplefilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterlist = mutableListOf<Reciter>()
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

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            if (filterResults.values != null) {
                listData.clear()
                listData.addAll(filterResults.values as MutableList<Reciter>)
                notifyDataSetChanged()
            }
        }
    }

    override fun getFilter(): Filter {

        return examplefilter

    }
}