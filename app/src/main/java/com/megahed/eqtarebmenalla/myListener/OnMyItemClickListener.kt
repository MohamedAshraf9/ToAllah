package com.megahed.eqtarebmenalla.myListener

import android.view.View

interface OnMyItemClickListener<T> {

    fun onItemClick(itemObject:T,view: View?,position: Int=0)
    fun onItemLongClick(itemObject:T,view: View?,position: Int=0)

}