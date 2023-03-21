package com.megahed.eqtarebmenalla.myListener

import android.view.View

interface OnItemReaderClickListener<T>:OnItemWithFavClickListener<T> {
    fun onItemClickReader(itemObject:T, view: View?, readerName:String)
}