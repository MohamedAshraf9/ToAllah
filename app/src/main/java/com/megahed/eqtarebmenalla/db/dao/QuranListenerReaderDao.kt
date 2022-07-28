package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.customModel.SorasFavOfReader
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranListenerReaderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuranListenerReader(quranListenerReader: QuranListenerReader)

    @Update
    suspend fun updateQuranListenerReader(quranListenerReader: QuranListenerReader)

    @Delete
    suspend fun deleteQuranListenerReader(quranListenerReader: QuranListenerReader)


    @Query("SELECT * FROM quranlistenerreader WHERE id =:id ")
    suspend fun getQuranListenerReaderById(id:String): QuranListenerReader?


    @Query("SELECT * FROM quranlistenerreader WHERE isVaForte=1 ")
    fun getFavoriteQuranListenerReader(): Flow<List<QuranListenerReader>>

    @Query("SELECT * FROM quranlistenerreader ")
    fun getAllQuranListenerReader(): Flow<List<QuranListenerReader>>

    @Transaction
    @Query("SELECT q.* FROM quranlistenerreader q INNER JOIN sorasong s ON q.id=s.readerId and s.isVaForte=1 group by q.id")
    fun getAllFavSorasOfReader():Flow<List<ReaderWithSora>>

}