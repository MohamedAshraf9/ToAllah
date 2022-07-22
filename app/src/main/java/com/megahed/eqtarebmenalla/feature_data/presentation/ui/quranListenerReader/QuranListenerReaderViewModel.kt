package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import androidx.lifecycle.ViewModel
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QuranListenerReaderViewModel @Inject constructor(
    private val quranListenerReaderRepository: QuranListenerReaderRepository
) : ViewModel() {

    suspend fun getQuranListenerReaderById(id:String): QuranListenerReader?{
        return quranListenerReaderRepository.getQuranListenerReaderById(id)
    }

}