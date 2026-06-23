package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.ui.components.AppbarAction
import com.paperapps.paperscores.ui.components.ApplicationBar
import com.paperapps.paperscores.ui.components.DashedDivider
import com.paperapps.paperscores.ui.components.PanoramaHeader
import com.paperapps.paperscores.ui.viewmodel.GameDetailsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameDetailsScreen(
    matchId: String,
    onBackClick: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel()
) {
    val matchDetails by viewModel.matchDetails.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetails(matchId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp))

        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("box score", "stats", "lineups"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> BoxScoreTab(match)
                    1 -> StatsTab(match)
                    2 -> LineupsTab(match)
                }
            }
        }

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.Filled.ArrowBack,
                    label = "Back",
                    onClick = onBackClick
                ),
                AppbarAction(
                    icon = Icons.Filled.Refresh,
                    label = "Refresh",
                    onClick = { viewModel.loadMatchDetails(matchId) }
                )
            )
        )
    }
}

@Composable
fun BoxScoreTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val dateFormatted = try {
            val formatterIn = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm z", java.util.Locale.US)
            val zonedDateTime = java.time.ZonedDateTime.parse(match.matchTime, formatterIn)
            val formatterOut = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            zonedDateTime.format(formatterOut)
        } catch (e: Exception) {
            match.matchTime.split(",").take(3).joinToString(",").trim()
        }

        Text(dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        if (match.stadiumName.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(match.stadiumName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PureBlack)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

        // Home Team Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (match.homeTeam.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = match.homeTeam.imageUrl,
                    contentDescription = match.homeTeam.name,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp),
                    colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                )
            } else {
                Spacer(modifier = Modifier.width(52.dp))
            }

            Box(
                modifier = Modifier
                    .background(PureBlack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(match.homeTeam.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (match.score.home != null) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .background(PureBlack)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${match.score.home}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }

        // Away Team Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (match.awayTeam.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = match.awayTeam.imageUrl,
                    contentDescription = match.awayTeam.name,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp),
                    colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                )
            } else {
                Spacer(modifier = Modifier.width(52.dp))
            }

            Box(
                modifier = Modifier
                    .background(PureBlack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(match.awayTeam.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (match.score.away != null) {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .background(PureBlack)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${match.score.away}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(thickness = 2.dp, color = PureBlack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(match.events) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("${event.timeStr}'", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val formattedType = event.type.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                        Text(formattedType, fontWeight = FontWeight.Bold)

                        if (event.nameStr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.nameStr, fontSize = 14.sp, color = PureBlack)
                        }
                    }
                }
                DashedDivider()
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
            DashedDivider()
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
    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
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

