package com.benchopo.firering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.benchopo.firering.navigation.NavGraph
import com.benchopo.firering.ui.theme.FireRingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        // Test the new Firebase database structure
        // FirebaseTest.testNewDatabaseStructure()

        setContent {
            FireRingTheme { Surface(color = MaterialTheme.colorScheme.background) { NavGraph() } }
        }
    }
}
