package com.megahed.eqtarebmenalla.db.repository

import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import kotlinx.coroutines.flow.Flow

interface AzkarCategoryRepository {


    suspend fun insertAzkarCategory(azkarCategory: AzkarCategory):Long

    suspend fun updateAzkarCategory(azkarCategory: AzkarCategory)

    suspend fun deleteAzkarCategory(azkarCategory: AzkarCategory)

    fun getAllAzkarCategory(): Flow<List<AzkarCategory>>

}