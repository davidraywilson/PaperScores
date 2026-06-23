package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
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
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperscores.theme.EInkGrey

@Composable
fun TodaysGamesScreen(
    onGameClick: (String) -> Unit,
    viewModel: TodaysGamesViewModel = viewModel()
) {
    val todaysGames by viewModel.todaysGames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshGames()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
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
                    text = "No games for today for followed teams.",
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

@Composable
fun MatchCard(match: MatchDetails, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            TeamRow(team = match.homeTeam, score = match.score.home?.toString() ?: "")
            Spacer(modifier = Modifier.height(3.dp))
            TeamRow(team = match.awayTeam, score = match.score.away?.toString() ?: "")
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = match.tournamentName,
                    fontSize = 12.sp,
                    color = PureBlack,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                if (match.status !in listOf("Finished", "Active", "Upcoming") && match.liveTime.isNotBlank() && match.liveTime != match.status) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.End
                    ) {
                        Text(
                            text = match.liveTime,
                            fontSize = 12.sp,
                            color = PureBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = match.status,
                            fontSize = 12.sp,
                            color = PureBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = match.matchTime,
                        fontSize = 12.sp,
                        color = PureBlack,
                        fontWeight = if (match.status !in listOf("Finished", "Active", "Upcoming")) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TeamRow(team: com.paperapps.paperscores.network.models.Team, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://images.fotmob.com/image_resources/logo/teamlogo/${team.id}.png",
            contentDescription = team.name,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        Box(
            modifier = Modifier
                .background(PureBlack)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = team.name.uppercase(),
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .background(PureBlack)
                .defaultMinSize(minWidth = 28.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = score,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

