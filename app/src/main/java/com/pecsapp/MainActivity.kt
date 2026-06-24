package com.pecsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pecsapp.kiosk.KioskManager
import com.pecsapp.ui.screens.AppNavigation
import com.pecsapp.ui.theme.PecsAppTheme
import com.pecsapp.viewmodel.PecViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PecViewModel by viewModels()
    private lateinit var kioskManager: KioskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modo pantalla completa
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        kioskManager = KioskManager(this)
        if (!kioskManager.isInKioskMode()) {
            kioskManager.startKioskMode()
        }

        setContent {
            PecsAppTheme {
                var maxImages by remember { mutableIntStateOf(4) }
                var youtubeCycles by remember { mutableIntStateOf(5) }

                AppNavigation(
                    viewModel = viewModel,
                    maxImages = maxImages,
                    onMaxImagesChanged = { maxImages = it },
                    youtubeCycles = youtubeCycles,
                    onYoutubeCyclesChanged = { youtubeCycles = it }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !kioskManager.isInKioskMode()) {
            kioskManager.startKioskMode()
        }
    }
}