package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.ui.viewmodel.TodaysGamesViewModel
import com.paperapps.paperscores.ui.components.MatchCard
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TodaysGamesScreen(
    onGameClick: (String) -> Unit,
    viewModel: TodaysGamesViewModel = viewModel()
) {
    val todaysGames by viewModel.todaysGames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Day",
                tint = PureBlack,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { viewModel.previousDay() }
            )
            Text(
                text = format.format(selectedDate),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Day",
                tint = PureBlack,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { viewModel.nextDay() }
            )
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (isLoading && todaysGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.TopCenter
                ) {
                    Text(
                        text = "Loading...",
                        fontSize = 16.sp,
                        color = PureBlack
                    )
                }
            } else if (todaysGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.TopCenter
                ) {
                    Text(
                        text = "No games for this day for followed teams.",
                        fontSize = 16.sp,
                        color = PureBlack
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(todaysGames) { match ->
                        MatchCard(match = match, onClick = { onGameClick(match.matchId) })
                    }
                }
            }
        }
    }
}



