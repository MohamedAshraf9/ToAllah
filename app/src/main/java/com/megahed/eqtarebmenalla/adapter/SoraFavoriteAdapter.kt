package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.QuranListenerItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraFavItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter
import com.megahed.eqtarebmenalla.myListener.OnItemReaderClickListener
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.myListener.OnMyItemClickListener
import java.io.File
import java.util.*

class SoraFavoriteAdapter (private val context: Context,
                           private val onMyItemClickListener: OnItemReaderClickListener<SoraSong>
) : RecyclerView.Adapter<SoraFavoriteAdapter.MyHolder>(){

    private var listData= mutableListOf<ReaderWithSora>()

    @SuppressLint("NotifyDataSetChanged")
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
        val sFav= soraFav.soraSongData.filter {
            it.isVaForte
        }
        if (sFav.isNotEmpty()){

            holder.soraName.text=soraFav.quranListenerReader.name
            val quranListenerReaderAdapter =QuranListenerReaderAdapter(context,
                object :
                    OnItemWithFavClickListener<SoraSong> {

                    override fun onItemClick(itemObject: SoraSong, view: View?,position: Int) {
                        onMyItemClickListener.onItemClickReader(itemObject,view,soraFav.quranListenerReader.name)

                    }

                    override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                        onMyItemClickListener.onItemFavClick(itemObject,view)
                    }

                    override fun onItemLongClick(itemObject: SoraSong, view: View?,position: Int) {
                        downloadTask(itemObject,soraFav.quranListenerReader.name)
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



    }

    override fun getItemCount(): Int {
        return listData.size
    }


    @Throws(Exception::class)
    private fun downloadTask(soraSong: SoraSong,readerName:String): Boolean {
        if (!soraSong.url.startsWith("http")) {
            MethodHelper.toastMessage(context.getString(R.string.error))
            return false
        }
        try {

            val file= File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)
            if (!file.exists()) {
                file.mkdirs()
            }
            val result = File(file.absolutePath + File.separator.toString() + Constants.SORA_OF_QURAN[soraSong.SoraId]+"_"+(readerName?:"")+"."+soraSong.url.substringAfterLast('.', ""))
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(soraSong.url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            request.setDestinationUri(Uri.fromFile(result))
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,result.name)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadManager.enqueue(request)
            MethodHelper.toastMessage(context.getString(R.string.downloading))
            MediaScannerConnection.scanFile(
                context, arrayOf(result.toString()), null
            ) { path, uri ->
                MethodHelper.toastMessage(path)
            }


            //Log.d("jhdfgdjf", "yyyyyyyyyyyyyy")
        } catch (e: Exception) {
            MethodHelper.toastMessage(context.getString(R.string.error))
            //e.message?.let { Log.d("jhdfgdjf", it) }
            return false
        }
        return true
    }


}