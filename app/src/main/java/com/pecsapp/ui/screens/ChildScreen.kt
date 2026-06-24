package com.pecsapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.pecsapp.R
import com.pecsapp.data.model.PecImage
import com.pecsapp.viewmodel.PecViewModel
import kotlin.math.ceil
import kotlin.random.Random

private data class GridSpec(
    val columns: Int,
    val rows: Int
)

@Composable
fun ChildScreen(
    viewModel: PecViewModel,
    maxImages: Int = 4,
    onYoutubeRequested: () -> Unit,
    onNeedNotified: (String) -> Unit,
    onAdminAccess: () -> Unit      // 👈 nuevo parametro
) {
    val allImages by viewModel.images.collectAsState()
    val visibleImages = allImages.filter { !it.isYoutube }.take(maxImages)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        val density = LocalDensity.current
        val imageCount = visibleImages.size.coerceAtLeast(1)
        val isPortrait = maxHeight > maxWidth
        val gridSpec = remember(imageCount, isPortrait) {
            gridSpecFor(imageCount = imageCount, isPortrait = isPortrait)
        }
        val gridPadding = if (maxWidth < 420.dp) 6.dp else 8.dp
        val gridSpacing = if (maxWidth < 420.dp) 6.dp else 8.dp
        val youtubeTileSize = if (maxWidth < 420.dp) 48.dp else 56.dp

        val itemHeight = remember(maxHeight, gridPadding, gridSpacing, gridSpec.rows) {
            val available = maxHeight - (gridPadding * 2) - (gridSpacing * (gridSpec.rows - 1))
            (available / gridSpec.rows).coerceAtLeast(72.dp)
        }

        val youtubeOffset = remember(maxWidth, maxHeight) {
            val maxX = (maxWidth - youtubeTileSize).coerceAtLeast(0.dp)
            val maxY = (maxHeight - youtubeTileSize).coerceAtLeast(0.dp)

            val xPx = with(density) {
                maxX.roundToPx()
            }
            val yPx = with(density) {
                maxY.roundToPx()
            }

            IntOffset(
                x = if (xPx == 0) 0 else Random.nextInt(0, xPx + 1),
                y = if (yPx == 0) 0 else Random.nextInt(0, yPx + 1)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSpec.columns),
            contentPadding = PaddingValues(gridPadding),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleImages) { image ->
                PecImageCard(
                    image = image,
                    itemHeight = itemHeight,
                    onClick = {
                        viewModel.onImageTapped(image)
                        onNeedNotified(image.label)
                    }
                )
            }
        }

        FloatingYoutubeButton(
            size = youtubeTileSize,
            offset = youtubeOffset,
            onClick = onYoutubeRequested
        )

        // Acceso discreto a administración: toque o pulsación larga en el ícono.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(if (maxWidth < 420.dp) 36.dp else 40.dp)
                .combinedClickable(
                    onClick = { onAdminAccess() },
                    onLongClick = { onAdminAccess() }
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Administración",
                tint = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PecImageCard(
    image: PecImage,
    itemHeight: Dp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16213E)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(image.imagePath),
                contentDescription = image.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop
            )
            Text(
                text = image.label,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
            )
        }
    }
}

private fun gridSpecFor(imageCount: Int, isPortrait: Boolean): GridSpec {
    val count = imageCount.coerceAtLeast(1)
    val preset = if (isPortrait) {
        when (count) {
            1 -> GridSpec(columns = 1, rows = 1)
            2 -> GridSpec(columns = 1, rows = 2)
            3, 4 -> GridSpec(columns = 2, rows = 2)
            5, 6 -> GridSpec(columns = 2, rows = 3)
            7, 8 -> GridSpec(columns = 2, rows = 4)
            9, 10 -> GridSpec(columns = 2, rows = 5)
            11, 12 -> GridSpec(columns = 3, rows = 4)
            else -> {
                val columns = 3
                val rows = ceil(count / columns.toDouble()).toInt()
                GridSpec(columns = columns, rows = rows)
            }
        }
    } else {
        when (count) {
            1 -> GridSpec(columns = 1, rows = 1)
            2 -> GridSpec(columns = 2, rows = 1)
            3, 4 -> GridSpec(columns = 2, rows = 2)
            5, 6 -> GridSpec(columns = 3, rows = 2)
            7, 8 -> GridSpec(columns = 4, rows = 2)
            9, 10 -> GridSpec(columns = 5, rows = 2)
            11, 12 -> GridSpec(columns = 4, rows = 3)
            else -> {
                val rows = 3
                val columns = ceil(count / rows.toDouble()).toInt()
                GridSpec(columns = columns, rows = rows)
            }
        }
    }
    val normalizedRows = ceil(count / preset.columns.toDouble()).toInt().coerceAtLeast(1)
    return GridSpec(columns = preset.columns, rows = normalizedRows)
}

@Composable
fun FloatingYoutubeButton(
    size: Dp,
    offset: IntOffset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .offset { offset }
            .size(size)
            .clickable { onClick() },
        shape = RoundedCornerShape(size / 5),
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_youtube_logo),
                contentDescription = "YouTube",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}