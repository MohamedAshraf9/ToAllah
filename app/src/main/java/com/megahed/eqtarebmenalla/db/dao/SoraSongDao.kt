package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.SoraSong
import kotlinx.coroutines.flow.Flow

@Dao
interface SoraSongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSoraSong(soraSong: SoraSong)

    @Update
    suspend fun updateSoraSong(soraSong: SoraSong)

    @Delete
    suspend fun deleteSoraSong(soraSong: SoraSong)


    @Query("SELECT * FROM sorasong WHERE SoraId =:id and readerId=:readerId ")
    suspend fun getSoraSongById(id:Int,readerId:String): SoraSong?


    @Query("SELECT * FROM sorasong WHERE isVaForte=1 ")
    fun getFavoriteSoraSong(): Flow<List<SoraSong>>

    @Query("SELECT * FROM sorasong WHERE readerId=:readerId order by SoraId")
    fun getSongsOfSora(readerId:String): Flow<List<SoraSong>>

}