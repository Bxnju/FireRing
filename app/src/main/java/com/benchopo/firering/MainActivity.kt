package com.benchopo.firering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.benchopo.firering.navigation.NavGraph
import com.benchopo.firering.ui.theme.FireRingTheme
import com.benchopo.firering.viewmodel.ConnectionViewModel
import com.benchopo.firering.viewmodel.GameViewModel
import com.benchopo.firering.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FireRingTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    // Create ViewModels at the app root level
                    val userViewModel: UserViewModel = viewModel()
                    val gameViewModel: GameViewModel = viewModel { GameViewModel(userViewModel) }
                    val connectionViewModel: ConnectionViewModel = viewModel {
                        ConnectionViewModel(userViewModel, gameViewModel)
                    }

                    // Pass all ViewModels to NavGraph
                    NavGraph(
                            userViewModel = userViewModel,
                            gameViewModel = gameViewModel,
                            connectionViewModel = connectionViewModel
                    )
                }
            }
        }
    }
}
