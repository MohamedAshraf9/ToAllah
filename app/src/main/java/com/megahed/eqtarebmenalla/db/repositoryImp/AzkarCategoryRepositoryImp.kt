package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.AzkarCategoryDao
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.repository.AzkarCategoryRepository
import kotlinx.coroutines.flow.Flow

class AzkarCategoryRepositoryImp(
    private val azkarCategoryDao: AzkarCategoryDao
):AzkarCategoryRepository {
    override suspend fun insertAzkarCategory(azkarCategory: AzkarCategory):Long {
       return azkarCategoryDao.insertAzkarCategory(azkarCategory)
    }

    override suspend fun updateAzkarCategory(azkarCategory: AzkarCategory) {
        azkarCategoryDao.updateAzkarCategory(azkarCategory)
    }

    override suspend fun deleteAzkarCategory(azkarCategory: AzkarCategory) {
        azkarCategoryDao.deleteAzkarCategory(azkarCategory)
    }

    override fun getAllAzkarCategory(): Flow<List<AzkarCategory>> {
        return azkarCategoryDao.getAllAzkarCategory()
    }
}