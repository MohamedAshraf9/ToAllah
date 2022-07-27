package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.common.CommonUtils
import com.megahed.eqtarebmenalla.databinding.AnaliticRowBinding
import com.megahed.eqtarebmenalla.databinding.TasbehAnalizeItemBinding
import com.megahed.eqtarebmenalla.db.customModel.HoursDate
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter

class TasbehAnalizeAdapter2 (private val context: Context
) : RecyclerView.Adapter<TasbehAnalizeAdapter2.MyHolder>() {

    private var listData= mutableListOf<TasbehCounter>()

    private var totalTasbeh=0L

    fun setData(data:List<TasbehCounter>, totalTasbeh:Long){
        listData.clear()
        listData.addAll(data)
        this.totalTasbeh=totalTasbeh
        notifyDataSetChanged()
    }


    class MyHolder(binding: AnaliticRowBinding) : RecyclerView.ViewHolder(binding.root) {

        val percentPagesDone=binding.percentPagesDone
        val progressBar=binding.progressBar
        val month=binding.month

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(AnaliticRowBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val tasbeh= listData[position]

        holder.month.text= tasbeh.tasbehName
        holder.progressBar.max=totalTasbeh.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            holder.progressBar.setProgress(tasbeh.count.toInt(),true)
        else holder.progressBar.progress = tasbeh.count.toInt()
        holder.percentPagesDone.text="${tasbeh.count}"
        val percent=(tasbeh.count.toFloat()/totalTasbeh.toFloat())*100
        holder.percentPagesDone.text="${String.format("%.2f",percent)}%"


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}