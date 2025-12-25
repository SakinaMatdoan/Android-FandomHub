package com.kelompok1.fandomhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kelompok1.fandomhub.ui.theme.FandomHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Modern Edge-to-Edge
        
        val app = application as FandomApplication
        val repository = app.repository
        
        setContent {
            FandomHubTheme {
                FandomHubApp(repository = repository)
            }
        }
    }
}
