package com.megahed.eqtarebmenalla.feature_data.presentation.ui.azkar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.repository.AyaRepository
import com.megahed.eqtarebmenalla.db.repository.AzkarCategoryRepository
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AzkarCategoryViewModel@Inject constructor(
    private val azkarCategoryRepository: AzkarCategoryRepository
) : ViewModel() {

    fun getAllAzkarCategory(): Flow<List<AzkarCategory>>{
        return azkarCategoryRepository.getAllAzkarCategory()
    }


}