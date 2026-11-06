package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VoiceChangerMainApp
import com.example.ui.theme.VoiceChangerTheme
import com.example.viewmodel.VoiceChangerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceChangerTheme {
                val viewModel: VoiceChangerViewModel = viewModel()
                VoiceChangerMainApp(viewModel = viewModel)
            }
        }
    }
}
