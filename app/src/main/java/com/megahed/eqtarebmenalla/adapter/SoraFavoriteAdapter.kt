package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.QuranListenerItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraFavItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.util.*

class SoraFavoriteAdapter (private val context: Context,
                           private val onMyItemClickListener: OnMyItemClickListener<ReaderWithSora>
) : RecyclerView.Adapter<SoraFavoriteAdapter.MyHolder>(){

    private var listData= mutableListOf<ReaderWithSora>()

    fun setData(data: List<ReaderWithSora>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: SoraFavItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraName=binding.readerName
        val recyclerView=binding.recyclerView
        val cardContainer=binding.cardContainer

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(SoraFavItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val soraFav= listData[position]
        val sFav=  soraFav.soraSongData.filter {
            it.isVaForte
        }
        if (sFav.isNotEmpty()){

            holder.soraName.text=soraFav.quranListenerReader.name
            val quranListenerReaderAdapter =QuranListenerReaderAdapter(context,
                object :
                    OnItemWithFavClickListener<SoraSong> {

                    override fun onItemClick(itemObject: SoraSong, view: View?) {

                    }

                    override fun onItemFavClick(itemObject: SoraSong, view: View?) {

                    }

                    override fun onItemLongClick(itemObject: SoraSong, view: View?) {
                    }
                })

            val verticalLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            holder.recyclerView.layoutManager = verticalLayoutManager
            holder.recyclerView.setHasFixedSize(true)
            holder.recyclerView.adapter = quranListenerReaderAdapter
            quranListenerReaderAdapter.setData(sFav)
        }
        else{
            holder.cardContainer.visibility=View.GONE
        }

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}