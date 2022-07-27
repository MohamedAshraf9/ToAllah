package com.megahed.eqtarebmenalla.myListener

import android.view.View

interface OnTasbehEditListener<T> {

    fun onUpdateClick(newText:String,itemObject:T,view: View?)
    fun onDeleteClick(itemObject:T,view: View?)

}