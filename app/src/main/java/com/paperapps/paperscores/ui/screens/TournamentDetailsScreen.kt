package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.network.models.TournamentDetails
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperscores.ui.components.MatchCard
import com.paperapps.paperscores.ui.viewmodel.TournamentDetailsState
import com.paperapps.paperscores.ui.viewmodel.TournamentDetailsViewModel
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperui.components.PanoramaPager
import com.paperapps.paperui.components.PaperLazyColumn
import com.paperapps.paperscores.ui.components.TableView
import kotlinx.coroutines.launch

@Composable
fun TournamentDetailsScreen(
    tournamentId: String,
    onBackClick: () -> Unit,
    onGameClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    viewModel: TournamentDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFollowed by viewModel.isFollowed.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(tournamentId) {
        viewModel.loadTournamentDetails(tournamentId)
    }

    androidx.activity.compose.BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }

    val screenTitle = (uiState as? TournamentDetailsState.Success)?.details?.name

    Column(modifier = Modifier.fillMaxSize().background(PureWhite)) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("overview", "fixtures", "table"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            screenTitle = screenTitle
        )

        when (val state = uiState) {
            is TournamentDetailsState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text("Loading...", fontSize = 16.sp)
                }
            }
            is TournamentDetailsState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text(state.message, fontSize = 16.sp, color = EInkGrey)
                }
            }
            is TournamentDetailsState.Success -> {
                val details = state.details
                PanoramaPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> TournamentOverviewTab(details, onGameClick)
                        1 -> TournamentFixturesTab(details, onGameClick)
                        2 -> TournamentTableTab(details)
                    }
                }
            }
        }

        val details = (uiState as? TournamentDetailsState.Success)?.details

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    label = if (isFollowed) "Following" else "Follow",
                    onClick = {
                        if (details != null) {
                            viewModel.toggleFollow(tournamentId, details.name)
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
fun TournamentOverviewTab(details: TournamentDetails, onGameClick: (String) -> Unit) {
    if (details.overviewMatches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No recent matches available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        PaperLazyColumn(
            modifier = Modifier.fillMaxSize(),
            refreshKey = details,
        ) {
            items(details.overviewMatches) { match ->
                MatchCard(match = match, onClick = { onGameClick(match.matchId) })
            }
        }
    }
}

@Composable
fun TournamentFixturesTab(details: TournamentDetails, onGameClick: (String) -> Unit) {
    if (details.fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No upcoming fixtures available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        PaperLazyColumn(
            modifier = Modifier.fillMaxSize(),
            refreshKey = details,
        ) {
            items(details.fixtures) { match ->
                MatchCard(match = match, onClick = { onGameClick(match.matchId) })
            }
        }
    }
}

@Composable
fun TournamentTableTab(details: TournamentDetails) {
    if (details.table.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text("No table data available.", fontSize = 14.sp, color = EInkGrey)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TableView(details.table)
        }
    }
}
