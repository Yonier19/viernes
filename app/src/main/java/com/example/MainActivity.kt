package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.ViernesResidentService
import com.example.ui.AssistantViewModel
import com.example.ui.AuraNavScreen
import com.example.ui.MainScreen
import com.example.ui.MemoryScreen
import com.example.ui.PermissionsScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {

    private var assistantViewModel: AssistantViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuraTheme(darkTheme = true) {
                val vm: AssistantViewModel = viewModel()
                assistantViewModel = vm
                ViernesApp(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        assistantViewModel?.refreshPermissions()
        assistantViewModel?.listeningController?.onAppForegrounded()
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ViernesResidentService.EXTRA_TRIGGER_WAKE, false) == true) {
            intent.removeExtra(ViernesResidentService.EXTRA_TRIGGER_WAKE)
            assistantViewModel?.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        assistantViewModel?.listeningController?.onAppBackgrounded()
        assistantViewModel?.textToSpeechManager?.stop()
    }
}

@Composable
fun ViernesApp(viewModel: AssistantViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentScreen,
        label = "screen_navigation",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AuraNavScreen.HOME -> {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AuraNavScreen.MEMORY -> {
                MemoryScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AuraNavScreen.HOME) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AuraNavScreen.PERMISSIONS -> {
                PermissionsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AuraNavScreen.HOME) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AuraNavScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AuraNavScreen.HOME) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
