package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.Eya


class AyetAdapter(
    val context: Context,
    val listEya: ArrayList<Eya>,

) : RecyclerView.Adapter<AyetAdapter.ViewHolder>(){

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(eya: Eya){
            view.findViewById<TextView>(R.id.aya_id).text = convertNumberToArabicNB(eya.id.toString())
            view.findViewById<TextView>(R.id.aya).text = eya.eya

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.aya_item_mp3, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listEya[position])
    }

    override fun getItemCount(): Int {
        return listEya.size
    }

    private fun convertNumberToArabicNB(str: String): String {
        val arabic_zero_unicode = 1632

        val builder = StringBuilder()

        for (i in 0 until str.length) {
            builder.append((str[i].code - 48 + arabic_zero_unicode).toChar())
        }

        return builder.toString()
    }

}