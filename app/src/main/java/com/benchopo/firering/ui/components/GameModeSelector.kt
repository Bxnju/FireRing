package com.benchopo.firering.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.benchopo.firering.R
import com.benchopo.firering.model.GameMode

@Composable
fun GameModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Select Game Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            GameMode.entries.forEach { mode ->
                GameModeCard(
                    mode = mode,
                    isSelected = mode == selectedMode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }

        // Game mode description
        Text(
            getGameModeDescription(selectedMode),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
fun GameModeCard(
    mode: GameMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = getGameModeIcon(mode)),
                contentDescription = "Game mode icon",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                getGameModeName(mode),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions for game mode info
fun getGameModeName(mode: GameMode): String {
    return when (mode) {
        GameMode.NORMAL -> "Normal"
        GameMode.CAMIONERO -> "Camionero"
        GameMode.DESPECHADO -> "Despechado"
        GameMode.CALENTURIENTOS -> "Calenturientos"
        GameMode.MEDIA_COPAS -> "Media Copas"
    }
}

fun getGameModeDescription(mode: GameMode): String {
    return when (mode) {
        GameMode.NORMAL -> "Standard Ring of Fire with classic rules."
        GameMode.CAMIONERO -> "Hard-drinking mode with truckers' challenges."
        GameMode.DESPECHADO -> "For the heartbroken - drink your sorrows away!"
        GameMode.CALENTURIENTOS -> "Spicy challenges for mature players."
        GameMode.MEDIA_COPAS -> "For when you're already halfway there."
    }
}

fun getGameModeIcon(mode: GameMode): Int {
    return when (mode) {
        GameMode.NORMAL -> R.drawable.ic_beer
        GameMode.CAMIONERO -> R.drawable.ic_barrel
        GameMode.DESPECHADO -> R.drawable.ic_cup
        GameMode.CALENTURIENTOS -> R.drawable.ic_hot_lips
        GameMode.MEDIA_COPAS -> R.drawable.ic_wine_cup
    }
}