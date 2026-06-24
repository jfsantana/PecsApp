package com.pecsapp.ui.screens

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.pecsapp.viewmodel.PecViewModel

sealed class Screen {
    object Child : Screen()
    object Youtube : Screen()
    object Admin : Screen()
}

private data class YoutubeSession(
    val url: String? = null,
    val second: Float = 0f
)

private const val SCREEN_CHILD = "child"
private const val SCREEN_YOUTUBE = "youtube"
private const val SCREEN_ADMIN = "admin"

@Composable
fun AppNavigation(
    viewModel: PecViewModel,
    maxImages: Int,
    onMaxImagesChanged: (Int) -> Unit,
    youtubeCycles: Int,
    onYoutubeCyclesChanged: (Int) -> Unit
) {
    var currentScreenKey by rememberSaveable { mutableStateOf(SCREEN_CHILD) }
    var youtubeUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var youtubeSecond by rememberSaveable { mutableFloatStateOf(0f) }
    val youtubeSession = YoutubeSession(url = youtubeUrl, second = youtubeSecond)

    when (currentScreenKey) {
        SCREEN_CHILD -> {
            ChildScreen(
                viewModel = viewModel,
                maxImages = maxImages,
                onYoutubeRequested = {
                    if (viewModel.consumeYoutubeCycle()) {
                        currentScreenKey = SCREEN_YOUTUBE
                    }
                },
                onNeedNotified = { _ ->
                    // Por ahora solo registra el tap
                },
                onAdminAccess = {
                    currentScreenKey = SCREEN_ADMIN
                }
            )
        }

        SCREEN_YOUTUBE -> {
            YoutubeScreen(
                initialUrl = youtubeSession.url,
                initialSecond = youtubeSession.second,
                onPlaybackProgress = { url, second ->
                    youtubeUrl = url
                    youtubeSecond = second
                },
                onTimerFinished = {
                    currentScreenKey = SCREEN_CHILD
                }
            )
        }

        SCREEN_ADMIN -> {
            AdminScreen(
                viewModel = viewModel,
                maxImages = maxImages,
                onMaxImagesChanged = onMaxImagesChanged,
                youtubeCycles = youtubeCycles,
                onYoutubeCyclesChanged = onYoutubeCyclesChanged,
                onBack = {
                    currentScreenKey = SCREEN_CHILD
                }
            )
        }

        else -> {
            currentScreenKey = SCREEN_CHILD
        }
    }
}