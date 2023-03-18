package com.megahed.eqtarebmenalla.adapter

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import java.util.*


class QuranListenerReaderAdapter (private val context: Context,
                                  private val onMyItemClickListener: OnItemWithFavClickListener<SoraSong>
) : RecyclerView.Adapter<QuranListenerReaderAdapter.MyHolder>(), Filterable {

    private var listData= mutableListOf<SoraSong>()
    private var listDataSearch= mutableListOf<SoraSong>()

    fun setData(data: List<SoraSong>){
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: SoraListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val soraName=binding.soraName
        val soraNumber=binding.soraNumber
        //val addPlayList=binding.addPlayList
        val fav=binding.fav
        val download=binding.download
        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(SoraListenerItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val quranListener= listData[position]
        holder.soraName.text=Constants.SORA_OF_QURAN[quranListener.SoraId]
       // holder.soraName.text=Constants.SORA_OF_QURAN[quranListener]
        holder.soraNumber.text= "${quranListener.SoraId}"


        if (quranListener.isVaForte){
            holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
        }
        else{
            holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }


        holder.fav.setOnClickListener {//for favorite Item
            onMyItemClickListener.onItemFavClick(listData[position],it)
        }


        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position],it)

        }

        holder.download.setOnClickListener {
            saveFileToDownloadFolder(listData[position].url,
                        listData[position].readerId.toString(),
                            listData[position].SoraId.toString()
                                 )
        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


    private val examplefilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterlist = mutableListOf<SoraSong>()
            if (charSequence.isEmpty()) {
                filterlist.addAll(listDataSearch)
            } else {
                val filterPattern =
                    charSequence.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (item in listDataSearch) {
                    if (Constants.SORA_OF_QURAN[item.SoraId].lowercase().contains(filterPattern)) {
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
                listData.addAll(filterResults.values as MutableList<SoraSong>)
                notifyDataSetChanged()
            }
        }
    }

    override fun getFilter(): Filter {

        return examplefilter

    }

    fun saveFileToDownloadFolder(url: String, qar2eId: String, surahId: String) {

        try{
        val downloadmanager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        val uri = Uri.parse(url)

            var nameSuah =Constants.SORA_OF_QURAN.get(surahId.toInt() )
        val request = DownloadManager.Request(uri)
        request.setTitle(nameSuah)
        request.setDescription("Downloading")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.allowScanningByMediaScanner();
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"$qar2eId-$surahId")
        //request.setDestinationUri(Uri.parse("file://qor2an/test.mp3"))

      var downloadReference =  downloadmanager!!.enqueue(request)
            if (downloadReference != 0L) {
                Toast.makeText(context, "بدأ التحميل", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(context, " هناك مشكل ما", Toast.LENGTH_SHORT).show();
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }




}