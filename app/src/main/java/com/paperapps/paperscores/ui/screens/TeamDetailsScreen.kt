package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.paperapps.paperscores.network.models.TeamDetails
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperscores.ui.components.AppbarAction
import com.paperapps.paperscores.ui.components.ApplicationBar
import com.paperapps.paperscores.ui.components.PanoramaHeader
import com.paperapps.paperscores.ui.components.TableView
import com.paperapps.paperscores.ui.components.MatchScoreHeader
import com.paperapps.paperscores.ui.components.MatchCard
import com.paperapps.paperscores.ui.components.DashedDivider
import com.paperapps.paperscores.ui.viewmodel.TeamDetailsState
import com.paperapps.paperscores.ui.viewmodel.TeamDetailsViewModel

@Composable
fun TeamDetailsScreen(
    teamId: String,
    onBackClick: () -> Unit,
    onGameClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    viewModel: TeamDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(teamId) {
        viewModel.loadTeamDetails(teamId)
    }

    Column(modifier = Modifier.fillMaxSize().background(PureWhite)) {
        Spacer(modifier = Modifier.height(48.dp))

        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("overview", "fixtures", "table", "squad"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            peekPadding = 48.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is TeamDetailsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading team details...", fontSize = 16.sp, color = EInkGrey)
                    }
                }
                is TeamDetailsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", fontSize = 16.sp, color = PureBlack)
                    }
                }
                is TeamDetailsState.Success -> {
                    val details = state.details
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(end = 48.dp)
                    ) { page ->
                        when (page) {
                            0 -> OverviewTab(details, onGameClick, onTeamClick)
                            1 -> FixturesTab(details, onGameClick)
                            2 -> TableTab(details)
                            3 -> SquadTab(details)
                        }
                    }
                }
            }
        }

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Back",
                    onClick = onBackClick
                )
            )
        )
    }
}

@Composable
fun OverviewTab(details: TeamDetails, onGameClick: (String) -> Unit, onTeamClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = "https://images.fotmob.com/image_resources/logo/teamlogo/${details.id}.png",
                    contentDescription = details.name,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = details.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${details.country} • ${details.primaryLeagueName}",
                        fontSize = 14.sp,
                        color = EInkGrey
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        details.nextMatch?.let { nextMatch ->
            val matchDetails = details.fixtures.find { it.matchId == nextMatch.id }
            if (matchDetails != null) {
                item {
                    Text("Next Match", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    Spacer(modifier = Modifier.height(12.dp))
                    MatchCard(match = matchDetails, onClick = { onGameClick(matchDetails.matchId) })
                }
            } else {
                item {
                    Text("Next Match", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (nextMatch.isHome) "${details.name} vs ${nextMatch.opponentName}" else "${nextMatch.opponentName} vs ${details.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureBlack
                        )
                        
                        val formattedDate = try {
                            val formatterIn = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                .withZone(java.time.ZoneId.of("UTC"))
                            val zonedDateTime = java.time.ZonedDateTime.parse(nextMatch.date, formatterIn)
                            val formatterOut = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy \u2022 h:mm a")
                            zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).format(formatterOut)
                        } catch (e: Exception) {
                            try {
                                val formatterIn = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm z", java.util.Locale.US)
                                val zonedDateTime = java.time.ZonedDateTime.parse(nextMatch.date, formatterIn)
                                val formatterOut = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy \u2022 h:mm a")
                                zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).format(formatterOut)
                            } catch (e2: Exception) {
                                nextMatch.date
                            }
                        }
                        
                        Text(formattedDate, fontSize = 14.sp, color = PureBlack)
                    }
                    Text(nextMatch.tournamentName, fontSize = 12.sp, color = EInkGrey)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (details.teamForm.isNotEmpty()) {
            item {
                Text("Form", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val grayscaleMatrix = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                    details.teamForm.forEach { form ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val bgColor = when (form.resultString) {
                                "W" -> PureWhite
                                else -> PureBlack
                            }
                            val textColor = when (form.resultString) {
                                "W" -> PureBlack
                                else -> PureWhite
                            }
                            Box(
                                modifier = Modifier
                                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, PureBlack, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = form.score,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    // do not wrap the text
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (form.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = form.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(grayscaleMatrix)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FixturesTab(details: TeamDetails, onGameClick: (String) -> Unit) {
    if (details.fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No fixtures available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(details.fixtures) { match ->
                MatchCard(match = match, onClick = { onGameClick(match.matchId) })
            }
        }
    }
}

@Composable
fun TableTab(details: TeamDetails) {
    if (details.table.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No table data available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        TableView(details.table)
    }
}

@Composable
fun SquadTab(details: TeamDetails) {
    if (details.squad.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No squad data available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            details.squad.forEach { section ->
                item {
                    Text(
                        text = section.title.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    DashedDivider()
                }
                items(section.members) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val number = member.shirtNumber?.toString() ?: "-"
                        Text(
                            text = number,
                            modifier = Modifier.width(32.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureBlack
                        )
                        AsyncImage(
                            model = "https://images.fotmob.com/image_resources/playerimages/${member.id}.png",
                            contentDescription = member.name,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = member.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = PureBlack
                        )
                    }
                    DashedDivider()
                }
            }
        }
    }
}
