package com.innovatex.expensetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.innovatex.expensetracker.presentation.dashboard.DashboardViewModel
import com.innovatex.expensetracker.presentation.dashboard.MonthlyDashboardScreen
import com.innovatex.expensetracker.presentation.theme.ApexExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val openAiTabSignal = mutableIntStateOf(0)

    companion object {
        const val EXTRA_OPEN_AI_TAB = "com.innovatex.expensetracker.OPEN_AI_TAB"
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true ||
            permissions[Manifest.permission.READ_SMS] == true
        if (smsGranted) {
            dashboardViewModel.triggerHistoricalScanIfNeeded(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra(EXTRA_OPEN_AI_TAB, false)) openAiTabSignal.intValue++

        checkAndRequestPermissions()

        setContent {
            ApexExpenseTrackerTheme {
                MonthlyDashboardScreen(
                    viewModel = dashboardViewModel,
                    openAiTabSignal = openAiTabSignal.intValue,
                    onSettingsClick = {
                        // Open Notification Listener Settings to enable Email capture
                        runCatching {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                    onTransactionClick = { transaction ->
                        // Handled in detail sheet
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_AI_TAB, false)) openAiTabSignal.intValue++
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            dashboardViewModel.triggerHistoricalScanIfNeeded(this)
        }
    }
}
