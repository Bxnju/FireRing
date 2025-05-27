package com.benchopo.firering.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.R
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.GameViewModel

@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Title
                Text(
                        "FireRing",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                )

                Row {
                        Text(
                                "The RoF game ",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                        )

                        Image(
                                painter = painterResource(id = R.drawable.ic_beer),
                                contentDescription = "Beer Icon",
                                modifier = Modifier.size(48.dp)
                        )
                }

                Image(painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        "The drinking card game",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Create Room Button
                Button(
                        onClick = { navController.navigate(Routes.CREATE_ROOM) },
                        modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent // Hacemos el fondo del botón transparente
                        ),
                        contentPadding = PaddingValues() // Quitamos el padding interno del botón
                ) {
                        Box(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                                brush = Brush.linearGradient(
                                                        colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = "Create a Room",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))



                // Join Room Button
                Button(
                        onClick = { navController.navigate(Routes.JOIN_ROOM) },
                        modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp),
                        colors = ButtonDefaults
                                .buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                ){
                        Box(
                                modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                                brush = Brush.linearGradient(
                                                        colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = "Join a Room",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                )
                        }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Version info or other details can go here
                Text(
                        "Version 0.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}
