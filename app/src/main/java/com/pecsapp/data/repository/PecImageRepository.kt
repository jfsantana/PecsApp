package com.pecsapp.data.repository

import com.pecsapp.data.db.PecImageDao
import com.pecsapp.data.model.PecImage
import kotlinx.coroutines.flow.Flow

class PecImageRepository(private val dao: PecImageDao) {

    val allActiveImages: Flow<List<PecImage>> = dao.getAllActive()

    suspend fun insert(image: PecImage) {
        dao.insert(image)
    }

    suspend fun delete(image: PecImage) {
        dao.delete(image)
    }

    suspend fun incrementTapCount(id: Int) {
        dao.incrementTapCount(id)
    }

    suspend fun setActive(id: Int, isActive: Boolean) {
        dao.setActive(id, isActive)
    }
}