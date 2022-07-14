package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AzkarCategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAzkarCategory(azkarCategory: AzkarCategory):Long

    @Update
    suspend fun updateAzkarCategory(azkarCategory: AzkarCategory)

    @Delete
    suspend fun deleteAzkarCategory(azkarCategory: AzkarCategory)


    @Query("SELECT * FROM azkarcategory")
    fun getAllAzkarCategory(): Flow<List<AzkarCategory>>



}