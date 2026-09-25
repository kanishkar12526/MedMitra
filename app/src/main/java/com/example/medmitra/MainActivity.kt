package com.example.medmitra

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.medmitra.service.MedMitraForegroundService
import com.example.medmitra.ui.navigation.AppNavigation
import com.example.medmitra.ui.theme.MedMitraTheme

class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        safeStartForegroundService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()

        val permissionsToRequest = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(ungranted.toTypedArray())
        } else {
            safeStartForegroundService()
        }

        setContent {
            MedMitraTheme {
                AppNavigation()
            }
        }
    }

    private fun safeStartForegroundService() {
        try {
            MedMitraForegroundService.startService(this)
        } catch (e: Throwable) {
            Log.e("MainActivity", "Failed to start foreground service safely", e)
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }

                val modes = display?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val maxRefreshRateMode = modes.maxByOrNull { it.refreshRate }
                    if (maxRefreshRateMode != null) {
                        val layoutParams = window.attributes
                        if (layoutParams != null) {
                            layoutParams.preferredDisplayModeId = maxRefreshRateMode.modeId
                            window.attributes = layoutParams
                            Log.i("MainActivity", "High refresh rate mode set to ${maxRefreshRateMode.refreshRate} Hz (Mode ID ${maxRefreshRateMode.modeId})")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("MainActivity", "Failed to set high refresh rate mode", e)
            }
        }
    }
}
