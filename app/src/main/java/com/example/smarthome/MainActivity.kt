package com.example.smarthome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.smarthome.navigation.SmartHomeNavHost
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w("MainActivity", "POST_NOTIFICATIONS permission denied — alerts will be silent")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Request notification permission (Android 13+) ──────────────────
        askNotificationPermission()

        // ── Subscribe to FCM topic for iron-safety alerts ──────────────────
        // The Cloud Function sends to "smarthome_alerts" topic when an iron
        // is automatically cut off. This subscription makes sure this device
        // receives those heads-up notifications.
        FirebaseMessaging.getInstance().subscribeToTopic("smarthome_alerts")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "FCM topic subscription successful")
                } else {
                    Log.e("MainActivity", "FCM topic subscription failed", task.exception)
                }
            }

        setContent {
            SmartHomeTheme {
                SmartHomeNavHost()
            }
        }
    }

    private fun askNotificationPermission() {
        // POST_NOTIFICATIONS is only required on Android 13 (API 33)+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}