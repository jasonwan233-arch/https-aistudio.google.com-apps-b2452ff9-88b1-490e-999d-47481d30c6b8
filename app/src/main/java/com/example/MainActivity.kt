package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.SoundCraftApp
import com.example.ui.SoundViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SoundViewModel by viewModels()

    // Activity permission launcher to request mic accesses dynamically
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        micPermissionGrantedState = isGranted
    }

    private var micPermissionGrantedState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic check of hardware permissions state
        micPermissionGrantedState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SoundCraftApp(
                        viewModel = viewModel,
                        micPermissionGranted = micPermissionGrantedState,
                        onRequestMicPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
