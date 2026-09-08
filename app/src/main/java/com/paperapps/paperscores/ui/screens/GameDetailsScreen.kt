package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.paperapps.paperui.components.PaperLazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import com.paperapps.paperui.components.PanoramaPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tv
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperscores.ui.components.TableView
import com.paperapps.paperscores.ui.components.MatchScoreHeader
import com.paperapps.paperscores.ui.viewmodel.GameDetailsViewModel

@Composable
fun GameDetailsScreen(
    matchId: String,
    onBackClick: () -> Unit,
    onTeamClick: (String) -> Unit,
    viewModel: GameDetailsViewModel = viewModel()
) {
    val matchDetails by viewModel.matchDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetails(matchId)
    }

    androidx.activity.compose.BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val screenTitle = matchDetails?.let { "${it.homeTeam.name} vs ${it.awayTeam.name}" }

    Column(modifier = Modifier.fillMaxSize()) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("box score", "stats", "lineups", "tournament"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            screenTitle = screenTitle
        )

        if (matchDetails == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text("Loading...", fontSize = 16.sp)
            }
        } else {
            val match = matchDetails!!
            
            PanoramaPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> BoxScoreTab(match, onTeamClick)
                    1 -> StatsTab(match)
                    2 -> LineupsTab(match)
                    3 -> TournamentTab(viewModel)
                }
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.Filled.Refresh,
                    label = "Refresh",
                    isLoading = isLoading,
                    onClick = { viewModel.loadMatchDetails(matchId) }
                ),
                AppbarAction(
                    icon = Icons.Outlined.PushPin,
                    label = "Pin Score",
                    onClick = {
                        if (android.provider.Settings.canDrawOverlays(context)) {
                            val intent = android.content.Intent(context, com.paperapps.paperscores.service.ScoreOverlayService::class.java).apply {
                                putExtra("matchId", matchId)
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            ),
            pagerState = pagerState,
            onBack = onBackClick
        )
    }
}

@Composable
fun BoxScoreTab(match: com.paperapps.paperscores.network.models.MatchDetails, onTeamClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        MatchScoreHeader(match = match, onTeamClick = onTeamClick)

        Spacer(modifier = Modifier.height(24.dp))

        val filteredEvents = match.events.filter { it.type != "Half" }

        PaperLazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Reset pages whenever match data changes (live polling updates).
            refreshKey = match,
        ) {
            items(filteredEvents) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            val parts = event.timeStr.split("+")
                            if (parts.size == 2) {
                                append(parts[0])
                                withStyle(style = SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)) {
                                    append("+${parts[1]}'")
                                }
                            } else {
                                append("${event.timeStr}'")
                            }
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp)
                    )

                    Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        when (event.type) {
                            "Goal" -> androidx.compose.material3.Icon(
                                Icons.Default.SportsSoccer, 
                                contentDescription = "Goal", 
                                modifier = Modifier.size(20.dp)
                            )
                            "YellowCard", "RedCard", "Card" -> {
                                val isRed = event.type == "RedCard" || event.nameStr.contains("Red", ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(14.dp, 20.dp)
                                        .background(
                                            color = if (isRed) PureBlack else PureWhite,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .border(
                                            width = if (isRed) 0.dp else 2.dp,
                                            color = PureBlack,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                            "Substitution" -> androidx.compose.material3.Icon(
                                Icons.Default.SwapHoriz, 
                                contentDescription = "Substitution", 
                                modifier = Modifier.size(20.dp)
                            )
                            "VAR" -> androidx.compose.material3.Icon(
                                Icons.Default.Tv, 
                                contentDescription = "VAR", 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (event.nameStr.isNotBlank()) {
                            Text(event.nameStr, fontSize = 14.sp, color = PureBlack)
                        }
                    }
                }
            }
            if (match.events.isEmpty()) {
                item {
                    Text("No match events yet.", fontSize = 14.sp, color = PureBlack)
                }
            }
        }
    }
}

@Composable
fun StatsTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    PaperLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            ,
        refreshKey = match,
    ) {
        items(match.stats) { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.homeStat, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
                Text(stat.title, modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontSize = 14.sp)
                Text(stat.awayStat, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
        }
        if (match.stats.isEmpty()) {
            item {
                Text("No stats available.", fontSize = 14.sp, color = PureBlack)
            }
        }
    }
}

@Composable
fun LineupsTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            ,
    ) {
        // Home Lineup
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            val hLineup = match.homeLineup
            if (hLineup != null) {
                Text(match.homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Formation: ${hLineup.formation}", fontSize = 14.sp, color = PureBlack)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(hLineup.starters) { player ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${player.shirtNumber}. ", fontWeight = FontWeight.Bold)
                            Text(player.name)
                        }
                    }
                }
            } else {
                Text("Lineup not available")
            }
        }

        // Away Lineup
        Column(modifier = Modifier.weight(1f)) {
            val aLineup = match.awayLineup
            if (aLineup != null) {
                Text(match.awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Formation: ${aLineup.formation}", fontSize = 14.sp, color = PureBlack)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(aLineup.starters) { player ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${player.shirtNumber}. ", fontWeight = FontWeight.Bold)
                            Text(player.name)
                        }
                    }
                }
            } else {
                Text("Lineup not available")
            }
        }
    }
}

@Composable
fun TournamentTab(viewModel: GameDetailsViewModel) {
    val tournamentData by viewModel.tournamentData.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        when (val state = tournamentData) {
            is com.paperapps.paperscores.ui.viewmodel.TournamentState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading tournament...", fontSize = 14.sp, color = PureBlack)
                }
            }
            is com.paperapps.paperscores.ui.viewmodel.TournamentState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tournament data available.", fontSize = 14.sp, color = PureBlack)
                }
            }
            is com.paperapps.paperscores.ui.viewmodel.TournamentState.Table -> {
                TableView(state.entries)
            }
            is com.paperapps.paperscores.ui.viewmodel.TournamentState.Playoff -> {
                PlayoffBracketView(state.rounds)
            }
        }
    }
}



@Composable
fun PlayoffBracketView(rounds: List<com.paperapps.paperscores.network.models.PlayoffRound>) {
    PaperLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        refreshKey = rounds,
    ) {
        items(rounds) { round ->
            Text(
                text = round.roundName.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PureBlack,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            round.matchups.forEach { matchup ->
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    val homeWinner = matchup.winner == matchup.homeTeam
                    val awayWinner = matchup.winner == matchup.awayTeam
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = matchup.homeTeam,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (homeWinner) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = PureBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (matchup.homeScore.isNotBlank()) {
                            Text(
                                text = matchup.homeScore,
                                fontWeight = if (homeWinner) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = PureBlack
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = matchup.awayTeam,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (awayWinner) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = PureBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (matchup.awayScore.isNotBlank()) {
                            Text(
                                text = matchup.awayScore,
                                fontWeight = if (awayWinner) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = PureBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
