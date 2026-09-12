package com.onlinesoccer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.core.ui.theme.OnlineSoccerTheme
import com.onlinesoccer.app.core.ui.theme.loadThemeOverride
import com.onlinesoccer.app.core.ui.theme.persistTheme
import com.onlinesoccer.app.core.ui.theme.rememberThemeState
import com.onlinesoccer.app.ui.AppRoot
import com.onlinesoccer.app.ui.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Composable
    private fun SplashLogoOverlay(fertig: Boolean) {
        var visible by remember { mutableStateOf(true) }
        var mindestZeitVorbei by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(400)
            mindestZeitVorbei = true
        }
        LaunchedEffect(fertig, mindestZeitVorbei) {
            if (fertig && mindestZeitVorbei) visible = false
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = null,
                )
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val context = applicationContext
        setContent {
            val themeState = rememberThemeState(loadThemeOverride(context))
            val systemDark = isSystemInDarkTheme()
            val isDark = if (themeState.followSystem) systemDark else themeState.dark
            val cycleTheme = {
                themeState.setManual(!isDark)
                persistTheme(context, themeState)
            }
            OnlineSoccerTheme(darkTheme = isDark) {
                val appViewModel: AppViewModel = viewModel()
                val authState by appViewModel.authState.collectAsStateWithLifecycle()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppRoot(
                        themeDark = isDark,
                        themeFollowsSystem = themeState.followSystem,
                        onThemeCycle = cycleTheme,
                    )
                    SplashLogoOverlay(fertig = authState != AuthUiState.Restoring)
                }
            }
        }
    }
}