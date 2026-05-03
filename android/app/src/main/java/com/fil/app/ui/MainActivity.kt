package com.fil.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fil.app.data.ContributionPreferences
import com.fil.app.data.EmergencyPreferences
import com.fil.app.data.GaitRepository
import com.fil.app.service.FallDetectionService
import com.fil.app.service.GaitAnalysisService
import com.fil.app.ui.contribution.ContributionPopup
import com.fil.app.ui.gait.GaitDashboard
import com.fil.app.ui.settings.EmergencySettingsScreen
import com.fil.app.ui.theme.FilTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: EmergencyPreferences
    private lateinit var gaitRepository: GaitRepository
    private lateinit var contributionPrefs: ContributionPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled — service checks individually */ }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = EmergencyPreferences(this)
        gaitRepository = GaitRepository(this)
        contributionPrefs = ContributionPreferences(this)

        requestPermissions()

        setContent {
            FilTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val enabled by prefs.fallDetectionEnabled.collectAsState(initial = false)
                        val contacts by prefs.contacts.collectAsState(initial = emptyList())
                        val gaitMetrics by GaitAnalysisService.latestMetrics.collectAsState()
                        val gaitDrift by GaitAnalysisService.latestDrift.collectAsState()
                        val baseline by gaitRepository.baseline.collectAsState(initial = null)
                        val lastPrompt by contributionPrefs.lastPromptTimestamp
                            .collectAsState(initial = -1L)

                        var showContribution by remember { mutableStateOf(false) }
                        LaunchedEffect(lastPrompt) {
                            if (lastPrompt == -1L) return@LaunchedEffect
                            if (lastPrompt == 0L) {
                                contributionPrefs.initializeIfFirstLaunch()
                                return@LaunchedEffect
                            }
                            val elapsed = System.currentTimeMillis() - lastPrompt
                            if (elapsed >= ContributionPreferences.CADENCE_MILLIS) {
                                showContribution = true
                            }
                        }

                        if (showContribution) {
                            ContributionPopup(
                                onClose = {
                                    showContribution = false
                                    scope.launch { contributionPrefs.markPromptShown() }
                                },
                            )
                        }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Fil") },
                                    actions = {
                                        IconButton(onClick = {
                                            navController.navigate("settings")
                                        }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings",
                                            )
                                        }
                                    },
                                )
                            },
                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))

                                // --- Fall Detection Section ---
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.height(64.dp),
                                    tint = if (enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    },
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = if (enabled) "Fall Detection Active" else "Fall Detection Off",
                                    style = MaterialTheme.typography.headlineSmall,
                                )

                                if (contacts.isEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add emergency contacts in Settings to enable",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Enable",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.padding(8.dp))
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { newValue ->
                                            scope.launch {
                                                prefs.setFallDetectionEnabled(newValue)
                                            }
                                            if (newValue) {
                                                startFallDetection()
                                                startGaitAnalysis()
                                            } else {
                                                stopFallDetection()
                                                stopGaitAnalysis()
                                            }
                                        },
                                        enabled = contacts.isNotEmpty(),
                                    )
                                }

                                Text(
                                    text = "${contacts.size} emergency contact(s) configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // --- Gait Dashboard Section ---
                                GaitDashboard(
                                    metrics = gaitMetrics,
                                    baseline = baseline,
                                    drift = gaitDrift,
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    composable("settings") {
                        val contacts by prefs.contacts.collectAsState(initial = emptyList())
                        val message by prefs.smsMessage.collectAsState(initial = "")

                        EmergencySettingsScreen(
                            contacts = contacts,
                            smsMessage = message,
                            onSaveContacts = { scope.launch { prefs.saveContacts(it) } },
                            onSaveMessage = { scope.launch { prefs.saveSmsMessage(it) } },
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startFallDetection() {
        startForegroundService(Intent(this, FallDetectionService::class.java))
    }

    private fun stopFallDetection() {
        startService(
            Intent(this, FallDetectionService::class.java).apply {
                action = FallDetectionService.ACTION_STOP
            }
        )
    }

    private fun startGaitAnalysis() {
        startForegroundService(Intent(this, GaitAnalysisService::class.java))
    }

    private fun stopGaitAnalysis() {
        startService(
            Intent(this, GaitAnalysisService::class.java).apply {
                action = GaitAnalysisService.ACTION_STOP
            }
        )
    }
}
