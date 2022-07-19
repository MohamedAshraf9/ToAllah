package com.megahed.eqtarebmenalla.feature_data.presentation.ui.tasbeh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehData
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import com.megahed.eqtarebmenalla.db.repository.TasbehDataRepository
import com.megahed.eqtarebmenalla.db.repository.TasbehRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasbehViewModel @Inject constructor(
    private val tasbehRepository: TasbehRepository,
    private val tasbehDataRepository: TasbehDataRepository
) : ViewModel() {


     fun insertTasbeh(tasbeh: Tasbeh){
        viewModelScope.launch {
            tasbehRepository.insertTasbeh(tasbeh)
        }
    }

     fun updateTasbeh(tasbeh: Tasbeh){
         viewModelScope.launch {
             tasbehRepository.updateTasbeh(tasbeh)
         }
     }

     fun deleteTasbeh(tasbeh: Tasbeh){
         viewModelScope.launch {
             tasbehRepository.deleteTasbeh(tasbeh)
         }
     }

    fun getAllTasbeh(): Flow<List<Tasbeh>>{
        return tasbehRepository.getAllTasbeh()
    }

    fun TasbehWithData(): Flow<List<TasbehWithData>>{
        return tasbehRepository.TasbehWithData()
    }


     fun insertTasbehData(tasbehData: TasbehData){
         viewModelScope.launch {
             tasbehDataRepository.insertTasbehData(tasbehData)
         }
     }

     fun updateTasbehData(tasbehData: TasbehData){
         viewModelScope.launch {
             tasbehDataRepository.updateTasbehData(tasbehData)
         }
     }

     fun deleteTasbehData(tasbehData: TasbehData){
         viewModelScope.launch {
             tasbehDataRepository.deleteTasbehData(tasbehData)
         }
     }

    fun getAllTasbehData(): Flow<List<TasbehData>>{
        return tasbehDataRepository.getAllTasbehData()
    }

    suspend fun getTasbehDataToday(id:Int,str: Date, end: Date): TasbehData?{
        return tasbehDataRepository.getTasbehDataToday(id,str,end)
    }


}