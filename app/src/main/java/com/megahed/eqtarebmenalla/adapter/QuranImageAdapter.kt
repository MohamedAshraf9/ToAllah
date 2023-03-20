package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.databinding.QuranImageItemBinding
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImageItem

class QuranImageAdapter (private val context: Activity
) : RecyclerView.Adapter<QuranImageAdapter.MyHolder>() {

    private var listData= mutableListOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data:Array<String>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: QuranImageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val image=binding.image

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(QuranImageItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val aya= listData[position]

        val uri: Uri =
            Uri.parse(aya)

        //Glide.with(context).load(aya).dontTransform().error(R.drawable.allah).placeholder(R.drawable.allah).into(holder.image)

       GlideToVectorYou.justLoadImage(context, uri, holder.image)
//        Glide.with(context)
//            .load(uri)
//            .placeholder(R.drawable.allah)
//            .error(R.drawable.allah)
//            .into(holder.image)



    }

    override fun getItemCount(): Int {
        return listData.size
    }


}