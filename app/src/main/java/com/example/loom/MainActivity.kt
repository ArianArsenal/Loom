package com.example.loom

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.loom.ui.LoomMainScreen
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.ui.theme.LoomTheme
import com.example.loom.worker.CleanupScheduler
import com.example.loom.worker.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // 1. Properly delegate ViewModel at the Activity level to handle background Intents
    private val viewModel: HomeViewModel by viewModels()

    private val emergencyExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val file = File(cacheDir, "emergency_backup.json")
            if (file.exists()) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                file.delete()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CleanupScheduler.scheduleBackgroundCleanup(this)

        // Catch cold-boot App Shortcuts
        intent?.action?.let { handleShortcutIntent(it) }

        val emergencyFile = File(cacheDir, "emergency_backup.json")
        val hasEmergencyBackup = emergencyFile.exists()

        setContent {
            var showEmergencyDialog by remember { mutableStateOf(hasEmergencyBackup) }
            val appTheme by viewModel.appTheme.collectAsState()

            val darkTheme = when (appTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            LoomTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { isGranted ->
                            if (isGranted) NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                        }
                    )

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            NotificationScheduler.scheduleDailyReminder(context, 21, 0)
                        }
                    }

                    // Pass ViewModel to main screen
                    LoomMainScreen(viewModel = viewModel)

                    if (showEmergencyDialog) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("App Recovered from Crash", color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            text = { Text("Loom closed unexpectedly, but we successfully intercepted and saved your data before the crash.\n\nWould you like to export this emergency backup to your phone to keep it safe?") },
                            confirmButton = {
                                Button(onClick = {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                                    emergencyExportLauncher.launch("Loom_Emergency_Backup_$dateStr.json")
                                    showEmergencyDialog = false
                                }) { Text("Export Backup") }
                            },
                            dismissButton = {
                                TextButton(onClick = { emergencyFile.delete(); showEmergencyDialog = false }) { Text("Discard", color = Color.Gray) }
                            }
                        )
                    }
                }
            }
        }
    }

    // Catch hot-boot App Shortcuts (when the app is already open in the background)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.action?.let { handleShortcutIntent(it) }
    }

    // High-performance shortcut router
    private fun handleShortcutIntent(action: String) {
        when (action) {
            "com.example.loom.ACTION_LOG_WATER" -> {
                // Silently log water in the background and show a Toast
                viewModel.addWater()
                Toast.makeText(this, "Logged water 💧", Toast.LENGTH_SHORT).show()
                viewModel.clearShortcutAction()
            }
            "com.example.loom.ACTION_NEW_NOTE",
            "com.example.loom.ACTION_QUICK_TASK" -> {
                // Pass to the UI state so Compose can open the correct dialogs/tabs
                viewModel.setShortcutAction(action)
            }
        }
    }
}