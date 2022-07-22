package com.megahed.eqtarebmenalla.myListener

import android.view.View

interface OnItemWithFavClickListener<T>:OnMyItemClickListener<T> {
    fun onItemFavClick(itemObject:T,view: View?)
}