package com.example.loom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loom.ui.LoomMainScreen
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.ui.theme.LoomTheme
import com.example.loom.worker.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dynamic edge-to-edge that automatically respects Light/Dark themes!
        enableEdgeToEdge()

        setContent {
            // 1. Instantiate the viewModel at the top level of setContent
            val viewModel: HomeViewModel = hiltViewModel()

            // 2. Collect the theme preference
            val appTheme by viewModel.appTheme.collectAsState()

            // 3. Determine if the theme should be dark based on the setting
            val darkTheme = when (appTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System" fallback
            }

            // 4. Pass it into LoomTheme
            LoomTheme(darkTheme = darkTheme) {
                val context = LocalContext.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                        } else {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                    }
                }

                LoomMainScreen(viewModel = viewModel)
            }
        }
    }
}