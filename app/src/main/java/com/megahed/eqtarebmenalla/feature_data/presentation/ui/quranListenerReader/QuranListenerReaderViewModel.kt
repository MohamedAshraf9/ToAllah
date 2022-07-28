package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import com.megahed.eqtarebmenalla.db.repository.SoraSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranListenerReaderViewModel @Inject constructor(
    private val quranListenerReaderRepository: QuranListenerReaderRepository,
    private val songRepository: SoraSongRepository
) : ViewModel() {




    suspend fun getQuranListenerReaderById(id:String): QuranListenerReader?{
        return quranListenerReaderRepository.getQuranListenerReaderById(id)?.let {
            getData(it)
            it
        }
    }

    fun updateQuranListenerReader(quranListenerReader: QuranListenerReader){
        viewModelScope.launch {
            quranListenerReaderRepository.updateQuranListenerReader(quranListenerReader)
        }
    }


     fun insertSoraSong(soraSong: SoraSong){
        viewModelScope.launch {
            songRepository.insertSoraSong(soraSong)
        }
    }

     fun updateSoraSong(soraSong: SoraSong){
        viewModelScope.launch {
            songRepository.updateSoraSong(soraSong)
        }
    }


    suspend fun getSoraSongById(id:Int,readerId:String): SoraSong?{
        return songRepository.getSoraSongById(id,readerId)
    }

    fun getFavoriteSoraSong(): Flow<List<SoraSong>>{
        return songRepository.getFavoriteSoraSong()
    }

    fun getSongsOfSora(readerId:String): Flow<List<SoraSong>>{
        return songRepository.getSongsOfSora(readerId)
    }

    fun getAllFavSorasOfReader(): Flow<List<ReaderWithSora>> {
        return quranListenerReaderRepository.getAllFavSorasOfReader()
    }

    private fun getData(quranListenerReader: QuranListenerReader) {
        val arr= quranListenerReader.suras.split(",")
        val ints= arr.map { it.toInt() }
        ints.let {
            it.forEach {
                viewModelScope.launch {
                    songRepository.getSoraSongById(it,quranListenerReader.id)?.let {it1->
                        it1.readerId=quranListenerReader.id
                        it1.url=Constants.getSoraLink(quranListenerReader.server, it)
                        songRepository.updateSoraSong(it1)
                    }?:run {
                        val soraSong=SoraSong(
                            it,quranListenerReader.id,
                            Constants.getSoraLink(quranListenerReader.server, it),
                            false
                        )
                        songRepository.insertSoraSong(soraSong)
                    }
                }
            }
        }

    }


}