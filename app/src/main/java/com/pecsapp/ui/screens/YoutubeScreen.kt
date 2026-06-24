package com.pecsapp.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeScreen(
    initialUrl: String?,
    initialSecond: Float,
    onPlaybackProgress: (url: String, second: Float) -> Unit,
    onTimerFinished: () -> Unit
) {
    // Timer de 10 minutos = 600 segundos
    var secondsLeft by rememberSaveable { mutableIntStateOf(600) }
    var watchedSecond by rememberSaveable { mutableFloatStateOf(initialSecond.coerceAtLeast(0f)) }
    val defaultVideoId = "M7lc1UVf-VE"
    val savedVideoId = rememberSaveable(initialUrl) { extractYoutubeVideoId(initialUrl) ?: defaultVideoId }
    var currentVideoId by rememberSaveable { mutableStateOf(savedVideoId) }
    val resumeStartAt = rememberSaveable(initialSecond) { initialSecond.toInt().coerceAtLeast(0) }
    val initialWatchUrl = remember(currentVideoId, resumeStartAt) {
        buildWatchUrl(currentVideoId, resumeStartAt)
    }
    var currentPageUrl by rememberSaveable { mutableStateOf(initialWatchUrl) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1.seconds)
            secondsLeft--
            watchedSecond += 1f
        }
        onTimerFinished()
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    DisposableEffect(Unit) {
        onDispose {
            onPlaybackProgress(currentPageUrl, watchedSecond)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                var fallbackLoaded = false
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (!fallbackLoaded && request?.isForMainFrame == true) {
                                fallbackLoaded = true
                                view?.loadUrl(buildWatchUrl(currentVideoId, watchedSecond.toInt()))
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (!fallbackLoaded && request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                fallbackLoaded = true
                                view?.loadUrl("https://www.youtube.com")
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!url.isNullOrBlank()) {
                                currentPageUrl = url
                            }
                            val detectedId = extractYoutubeVideoId(url)
                            if (!detectedId.isNullOrBlank()) {
                                currentVideoId = detectedId
                            }
                        }
                    }
                    loadUrl(currentPageUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            }
        )

        Text(
            text = "%02d:%02d".format(minutes, seconds),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


private fun extractYoutubeVideoId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val patterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{6,})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})"),
        Regex("embed/([A-Za-z0-9_-]{6,})")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        val id = match?.groupValues?.getOrNull(1)
        if (!id.isNullOrBlank()) return id
    }
    return null
}

private fun buildWatchUrl(videoId: String, second: Int): String {
    val safeSecond = second.coerceAtLeast(0)
    return "https://m.youtube.com/watch?v=$videoId&t=${safeSecond}s"
}

