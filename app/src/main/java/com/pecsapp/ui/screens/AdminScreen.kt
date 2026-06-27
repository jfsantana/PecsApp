package com.pecsapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecsapp.data.model.PecImage
import com.pecsapp.viewmodel.PecViewModel
import kotlin.math.roundToInt

@Composable
fun AdminScreen(
    viewModel: PecViewModel,
    maxImages: Int,
    onMaxImagesChanged: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsState()
    val alertSoundOptions by viewModel.alertSoundOptions.collectAsState()
    val selectedAlertSoundKey by viewModel.selectedAlertSoundKey.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAlertSoundMenu by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var newImageLabel by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Algunos proveedores no permiten permiso persistente.
            }
        }
        selectedImageUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Volver")
            }
        }

        item {
            Text(
                text = "Panel Administrador",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Imágenes visibles (pares): $maxImages",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Slider(
                    value = maxImages.toFloat(),
                    onValueChange = {
                        val evenValue = (it / 2f).roundToInt() * 2
                        onMaxImagesChanged(evenValue.coerceIn(2, 12))
                    },
                    valueRange = 2f..12f,
                    steps = 4
                )
                Text(
                    text = "Solo números pares",
                    color = Color(0xFFB0B0C8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "YouTube: acceso permanente (sin límite)",
                    color = Color(0xFFB0B0C8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sonido de alerta",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    OutlinedButton(
                        onClick = { showAlertSoundMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedLabel = alertSoundOptions
                            .firstOrNull { it.key == selectedAlertSoundKey }
                            ?.label
                            ?: "Seleccionar sonido"
                        Text(selectedLabel)
                    }

                    DropdownMenu(
                        expanded = showAlertSoundMenu,
                        onDismissRequest = { showAlertSoundMenu = false }
                    ) {
                        alertSoundOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (option.key == selectedAlertSoundKey) {
                                            "✓ ${option.label}"
                                        } else {
                                            option.label
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.updateAlertSound(option.key)
                                    showAlertSoundMenu = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Al elegir uno, escucharás una vista previa",
                    color = Color(0xFFB0B0C8),
                    fontSize = 12.sp
                )
            }
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F3460)
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar imagen")
            }
        }

        items(images.filter { !it.isYoutube }) { image ->
            AdminImageRow(
                image = image,
                onDelete = { viewModel.deleteImage(image) },
                onToggleActive = {
                    viewModel.setImageActive(image.id, !image.isActive)
                }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva imagen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newImageLabel,
                        onValueChange = { newImageLabel = it },
                        label = { Text("Nombre visible") },
                        placeholder = { Text("Ej: Tomar Agua") },
                        supportingText = {
                            Text("Sugerencia: usa frases simples de acción")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { galleryLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (selectedImageUri != null)
                                "Imagen seleccionada ✓"
                            else
                                "Seleccionar de galería"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newImageLabel.isNotEmpty() && selectedImageUri != null) {
                            viewModel.insertImage(
                                PecImage(
                                    label = newImageLabel,
                                    imagePath = selectedImageUri.toString(),
                                    isYoutube = false
                                )
                            )
                            showAddDialog = false
                            newImageLabel = ""
                            selectedImageUri = null
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AdminImageRow(
    image: PecImage,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16213E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pulsaciones: ${image.tapCount}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = image.isActive,
                onCheckedChange = { onToggleActive() }
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.Red
                )
            }
        }
    }
}
