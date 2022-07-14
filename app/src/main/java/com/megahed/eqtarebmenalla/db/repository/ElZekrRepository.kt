package com.megahed.eqtarebmenalla.db.repository

import com.megahed.eqtarebmenalla.db.model.ElZekr
import kotlinx.coroutines.flow.Flow

interface ElZekrRepository {


    suspend fun insertElZekr(elZekr: ElZekr)

    suspend fun updateElZekr(elZekr: ElZekr)

    suspend fun deleteElZekr(elZekr: ElZekr)


    fun getElZekrOfCatId(id:Int): Flow<List<ElZekr>>

}