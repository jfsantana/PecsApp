package com.pecsapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pecsapp.data.db.AppDatabase
import com.pecsapp.data.model.PecImage
import com.pecsapp.data.repository.PecImageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PecViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PecImageRepository
    val images: StateFlow<List<PecImage>>

    // Cuantos ciclos de YouTube quedan hoy
    var youtubeCyclesLeft: Int = 5

    init {
        val dao = AppDatabase.getDatabase(application).pecImageDao()
        repository = PecImageRepository(dao)

        images = repository.allActiveImages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onImageTapped(image: PecImage) {
        viewModelScope.launch {
            repository.incrementTapCount(image.id)
        }
    }

    fun insertImage(image: PecImage) {
        viewModelScope.launch {
            repository.insert(image)
        }
    }

    fun deleteImage(image: PecImage) {
        viewModelScope.launch {
            repository.delete(image)
        }
    }

    fun setImageActive(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.setActive(id, isActive)
        }
    }

    fun consumeYoutubeCycle(): Boolean {
        return if (youtubeCyclesLeft > 0) {
            youtubeCyclesLeft--
            true
        } else {
            false
        }
    }
}