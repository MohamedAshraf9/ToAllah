package com.megahed.eqtarebmenalla.myListener

import android.view.View

interface OnMyItemClickListener<T> {

    fun onItemClick(itemObject:T,view: View?)
    fun onItemLongClick(itemObject:T,view: View?)

}