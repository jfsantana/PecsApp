package com.pecsapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pec_images")
data class PecImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String,           // nombre: "Comida", "Baño", "YouTube"
    val imagePath: String,       // ruta local de la foto
    val isYoutube: Boolean = false,
    val tapCount: Int = 0,       // veces que el niño la tocó
    val isActive: Boolean = true // visible en pantalla o no
)