package com.benchopo.firering.ui.screens

import android.media.MediaPlayer
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.R
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.GameViewModel

@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel) {
    val context = LocalContext.current
    var clickCount by remember { mutableStateOf(0) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var clickCountVersion by remember { mutableStateOf(0) }

    // Se limpia al salir
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Entered HomeScreen, clearing all game data")
        gameViewModel.clearGameData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "FireRing",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "The RoF game ",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.ic_beer),
                contentDescription = "Beer Icon",
                modifier = Modifier.size(48.dp)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(350.dp)
                .clickable (
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    clickCount++
                    if (clickCount >= 10) {
                        mediaPlayer = MediaPlayer.create(context, R.raw.alcohol_warning)
                        mediaPlayer?.start()
                        clickCount = 0
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "The drinking card game",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { navController.navigate(Routes.CREATE_ROOM) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
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

        Button(
            onClick = { navController.navigate(Routes.JOIN_ROOM) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
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
                    text = "Join a Room",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Made by Benchopo - All rights reserved © 2025",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Version 1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable {
                    clickCountVersion++
                    val remaining = 5 - clickCountVersion

                    if (clickCountVersion >= 3 && clickCountVersion < 5) {
                        Toast.makeText(
                            context,
                            "Touch $remaining ${if (remaining == 1) "more time" else "more times"}",
                            Toast.LENGTH_SHORT
                        ).apply {
                            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
                        }.show()
                    }

                    if (clickCountVersion == 5) {
                        Toast.makeText(
                            context,
                            "You have unlocked the Bo' Rai Cho mode",
                            Toast.LENGTH_LONG
                        ).apply {
                            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
                        }.show()
                        clickCountVersion = 0
                    }
                }
                .padding(top = 8.dp)
        )
    }
}
