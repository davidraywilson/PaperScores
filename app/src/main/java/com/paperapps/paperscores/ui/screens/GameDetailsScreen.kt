package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.ui.viewmodel.GameDetailsViewModel
import com.paperapps.paperscores.ui.components.DashedDivider
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    matchId: String,
    onBackClick: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel()
) {
    val matchDetails by viewModel.matchDetails.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Stats", "Lineups")

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetails(matchId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (matchDetails == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                TextMMD("Loading...", fontSize = 16.sp)
            }
        } else {
            val match = matchDetails!!
            
            // Match Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        val dateFormatted = try {
                            val formatterIn = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm z", java.util.Locale.US)
                            val zonedDateTime = java.time.ZonedDateTime.parse(match.matchTime, formatterIn)
                            val formatterOut = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
                            zonedDateTime.format(formatterOut)
                        } catch (e: Exception) {
                            try {
                                val instant = java.time.Instant.parse(match.matchTime)
                                val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
                                zonedDateTime.format(formatter)
                            } catch (e2: Exception) {
                                match.matchTime.split(",").take(3).joinToString(",").trim()
                            }
                        }
                        
                        TextMMD(dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        if (match.stadiumName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextMMD(match.stadiumName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        if (match.status !in listOf("Finished", "Active", "Upcoming") && match.liveTime.isNotBlank() && match.liveTime != match.status) {
                            TextMMD(match.liveTime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            TextMMD(match.status, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008950)) // Green color for interruption similar to fotmob
                        } else {
                            val statusText = when (match.status) {
                                "Finished" -> "FT"
                                "Active", "Upcoming" -> if (match.liveTime.isNotBlank()) match.liveTime else match.status
                                else -> match.status
                            }
                            TextMMD(statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (match.status !in listOf("Finished", "Active", "Upcoming")) Color(0xFF008950) else Color.Black)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

                // Home Team Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (match.homeTeam.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = match.homeTeam.imageUrl,
                            contentDescription = match.homeTeam.name,
                            modifier = Modifier.size(40.dp).padding(end = 12.dp),
                            colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(40.dp).padding(end = 12.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color.Black)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        TextMMD(match.homeTeam.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (match.score.home != null) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .background(Color.Black)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextMMD("${match.score.home}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                            modifier = Modifier.size(40.dp).padding(end = 12.dp),
                            colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(40.dp).padding(end = 12.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color.Black)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        TextMMD(match.awayTeam.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (match.score.away != null) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .background(Color.Black)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextMMD("${match.score.away}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Tabs
            PrimaryTabRowMMD(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    TabMMD(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            TextMMD(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTabIndex) {
                    0 -> OverviewTab(match)
                    1 -> StatsTab(match)
                    2 -> LineupsTab(match)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(match.events) { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                TextMMD("${event.timeStr}'", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val formattedType = event.type.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                    TextMMD(formattedType, fontWeight = FontWeight.SemiBold)
                    
                    if (event.nameStr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextMMD(event.nameStr, fontSize = 14.sp, color = Color.Black)
                    }
                }
            }
            DashedDivider()
        }
        if (match.events.isEmpty()) {
            item {
                TextMMD("No match events yet.", fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun StatsTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(match.stats) { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextMMD(stat.homeStat, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
                TextMMD(stat.title, modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontSize = 14.sp)
                TextMMD(stat.awayStat, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
            DashedDivider()
        }
        if (match.stats.isEmpty()) {
            item {
                TextMMD("No stats available.", fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun LineupsTab(match: com.paperapps.paperscores.network.models.MatchDetails) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Home Lineup
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            val hLineup = match.homeLineup
            if (hLineup != null) {
                TextMMD(match.homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextMMD("Formation: ${hLineup.formation}", fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(hLineup.starters) { player ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            TextMMD("${player.shirtNumber}. ", fontWeight = FontWeight.Bold)
                            TextMMD(player.name)
                        }
                    }
                }
            } else {
                TextMMD("Lineup not available")
            }
        }
        
        // Away Lineup
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            val aLineup = match.awayLineup
            if (aLineup != null) {
                TextMMD(match.awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextMMD("Formation: ${aLineup.formation}", fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(aLineup.starters) { player ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            TextMMD("${player.shirtNumber}. ", fontWeight = FontWeight.Bold)
                            TextMMD(player.name)
                        }
                    }
                }
            } else {
                TextMMD("Lineup not available")
            }
        }
    }
}
