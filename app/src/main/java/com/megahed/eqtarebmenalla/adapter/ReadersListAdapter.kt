package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.ReaderItemBinding
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.util.Locale

class ReadersListAdapter(private val context: Context,
                         private val onMyItemClickListener: OnMyItemClickListener<QuranListenerReader>
) : RecyclerView.Adapter<ReadersListAdapter.MyHolder>(), Filterable {

    private var listData= mutableListOf<QuranListenerReader>()
    private var listDataSearch= mutableListOf<QuranListenerReader>()

    fun setData(data: List<QuranListenerReader>){
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: ReaderItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val letter=binding.letter
        val readerName=binding.readerName
        val rewayaInfo=binding.rewayaInfo

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(ReaderItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val quranListener= listData[position]

        holder.readerName.text=quranListener.name
        holder.letter.text=quranListener.letter
        val s= App.getInstance().getString(R.string.sora)
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
            val filterlist = mutableListOf<QuranListenerReader>()
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
                listData.addAll(filterResults.values as MutableList<QuranListenerReader>)
                notifyDataSetChanged()
            }
        }
    }

    override fun getFilter(): Filter {

        return examplefilter

    }
}