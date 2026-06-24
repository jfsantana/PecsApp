package com.pecsapp.data.db

import androidx.room.*
import com.pecsapp.data.model.PecImage
import kotlinx.coroutines.flow.Flow

@Dao
interface PecImageDao {

    @Query("SELECT * FROM pec_images WHERE isActive = 1 ORDER BY tapCount DESC")
    fun getAllActive(): Flow<List<PecImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: PecImage)

    @Delete
    suspend fun delete(image: PecImage)

    @Query("UPDATE pec_images SET tapCount = tapCount + 1 WHERE id = :id")
    suspend fun incrementTapCount(id: Int)

    @Query("UPDATE pec_images SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Int, isActive: Boolean)
}