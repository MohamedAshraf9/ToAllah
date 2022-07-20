package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.QuranListenerItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.util.*

class QuranListenerReaderAdapter (private val context: Context,
                                  private val onMyItemClickListener: OnMyItemClickListener<Song>
) : RecyclerView.Adapter<QuranListenerReaderAdapter.MyHolder>(), Filterable {

    private var listData= mutableListOf<Song>()
    private var listDataSearch= mutableListOf<Song>()

    fun setData(data:List<Song>){
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: SoraListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraName=binding.soraName
        val soraNumber=binding.soraNumber
        val addPlayList=binding.addPlayList
        val fav=binding.fav
        val download=binding.download

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(SoraListenerItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val quranListener= listData[position]
        holder.soraName.text=quranListener.title
       // holder.soraName.text=Constants.SORA_OF_QURAN[quranListener]
        holder.soraNumber.text= quranListener.mediaId


        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


    private val examplefilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterlist = mutableListOf<Song>()
            if (charSequence.isEmpty()) {
                filterlist.addAll(listDataSearch)
            } else {
                val filterPattern =
                    charSequence.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (item in listDataSearch) {
                    if (item.title.lowercase().contains(filterPattern)) {
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
                listData.addAll(filterResults.values as MutableList<Song>)
                notifyDataSetChanged()
            }
        }
    }

    override fun getFilter(): Filter {

        return examplefilter

    }
}