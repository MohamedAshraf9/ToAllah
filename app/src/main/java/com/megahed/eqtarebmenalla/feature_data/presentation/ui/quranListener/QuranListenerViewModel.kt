package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.toQuranListenerReader
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.QuranListenerUsesCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranListenerViewModel @Inject constructor(
    private val usesCase: QuranListenerUsesCase,
    private val quranListenerReaderRepository: QuranListenerReaderRepository
) : ViewModel() {

    //private val _state= MutableStateFlow(QuranListenerListState())
    //val state : StateFlow<QuranListenerListState> =_state

    init {
        usesCase().onEach {
            when(it){
                is Resource.Success ->{
                    it.data?.reciters?.forEach {
                        quranListenerReaderRepository.getQuranListenerReaderById(it.id)?.let {it1->
                            it1.isVaForte=false
                           if (!it1.equals(it.toQuranListenerReader())){
                               it1.id=it.id
                               it1.count=it.count
                               it1.rewaya=it.rewaya
                               it1.server=it.server
                               it1.name=it.name
                               it1.letter=it.letter
                               it1.suras=it.suras
                               it1.isVaForte=it1.isVaForte
                               quranListenerReaderRepository.updateQuranListenerReader(it1)
                           }
                        }?:run {
                            quranListenerReaderRepository.insertQuranListenerReader(it.toQuranListenerReader())
                        }
                    }
                   // _state.value= it.data?.let { it1 -> QuranListenerListState(reciter = it1.reciters) }!!

                }
                is Resource.Loading ->{
                    //_state.value= QuranListenerListState(isLoading = true)
                }
                is Resource.Error ->{
                    //_state.value=QuranListenerListState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)

    }

     fun updateQuranListenerReader(quranListenerReader: QuranListenerReader){
        viewModelScope.launch {
            quranListenerReaderRepository.updateQuranListenerReader(quranListenerReader)
        }
    }

    fun getFavoriteQuranListenerReader(): Flow<List<QuranListenerReader>>{
        return quranListenerReaderRepository.getFavoriteQuranListenerReader()
    }
    fun getAllQuranListenerReader(): Flow<List<QuranListenerReader>>{
        return quranListenerReaderRepository.getAllQuranListenerReader()
    }

    fun getAllFavSorasOfReader(): Flow<List<ReaderWithSora>> {
        return quranListenerReaderRepository.getAllFavSorasOfReader()
    }



}