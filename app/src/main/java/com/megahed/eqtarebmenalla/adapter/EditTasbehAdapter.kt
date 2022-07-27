package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.databinding.EditTasbehItemBinding
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.myListener.OnTasbehEditListener

class EditTasbehAdapter (private val context: Context,
                         private val onMyItemClickListener: OnTasbehEditListener<Tasbeh>
) : RecyclerView.Adapter<EditTasbehAdapter.MyHolder>() {

    private var listData= mutableListOf<Tasbeh>()

    fun setData(data:List<Tasbeh>){
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }


    class MyHolder(binding: EditTasbehItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val editTasbehText=binding.editTasbehText
        val update=binding.update
        val delete=binding.delete

        val root = binding.root



    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return MyHolder(EditTasbehItemBinding.inflate(LayoutInflater.from(context), parent, false))


    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val tasbeh= listData[position]

        holder.editTasbehText.editText?.setText(tasbeh.tasbehName)


        holder.update.setOnClickListener {//for update Item
            onMyItemClickListener.onUpdateClick( holder.editTasbehText.editText?.text.toString(),listData[position],it)

        }

        holder.delete.setOnClickListener {//for delete Item
            onMyItemClickListener.onDeleteClick(listData[position],it)

        }


    }

    override fun getItemCount(): Int {
        return listData.size
    }


}