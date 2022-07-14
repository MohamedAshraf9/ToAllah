package com.megahed.eqtarebmenalla.feature_data.presentation.ui.elzekr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ElzekrViewModel@Inject constructor(
    private val elZekrRepository: ElZekrRepository
) : ViewModel() {

     fun insertElZekr(elZekr: ElZekr){
         viewModelScope.launch {
             elZekrRepository.insertElZekr(elZekr)
         }
     }

     fun updateElZekr(elZekr: ElZekr){
         viewModelScope.launch {
             elZekrRepository.updateElZekr(elZekr)
         }
     }

     fun deleteElZekr(elZekr: ElZekr){
         viewModelScope.launch {
             elZekrRepository.deleteElZekr(elZekr)
         }
     }


    fun getElZekrOfCatId(id:Int): Flow<List<ElZekr>>{
        return elZekrRepository.getElZekrOfCatId(id)
    }


}