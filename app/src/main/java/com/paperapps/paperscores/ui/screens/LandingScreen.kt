package com.paperapps.paperscores.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import com.paperapps.paperui.components.PanoramaPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.repository.SoccerRepository
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperscores.ui.viewmodel.TodaysGamesViewModel
import com.paperapps.paperscores.ui.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandingScreen(
    onGameClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    todaysGamesViewModel: TodaysGamesViewModel = viewModel(),
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isToday by todaysGamesViewModel.isToday.collectAsState()
    val isSearchBarVisible by userProfileViewModel.isSearchBarVisible.collectAsState()
    val isLoading by todaysGamesViewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("games", "profile", "settings"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            screenTitle = "PaperScores"
        )

        PanoramaPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> TodaysGamesScreen(onGameClick = onGameClick, viewModel = todaysGamesViewModel)
                1 -> UserProfileScreen(onTeamClick = onTeamClick, viewModel = userProfileViewModel)
                2 -> SettingsScreen()
            }
        }

        val actions = mutableListOf<AppbarAction>()
        
        if (!isToday && pagerState.currentPage == 0) {
            actions.add(
                AppbarAction(
                    icon = Icons.Filled.DateRange,
                    label = "Today",
                    onClick = {
                        todaysGamesViewModel.returnToToday()
                    }
                )
            )
        }
        
        actions.add(
            AppbarAction(
                icon = Icons.Filled.Refresh,
                label = "Refresh",
                isLoading = isLoading,
                onClick = {
                    Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
                    todaysGamesViewModel.refreshGames(forceRefresh = true)
                }
            )
        )
        
        if (pagerState.currentPage == 1) {
            actions.add(
                AppbarAction(
                    icon = if (isSearchBarVisible) Icons.Filled.Close else Icons.Filled.Search,
                    label = if (isSearchBarVisible) "Close" else "Search",
                    onClick = {
                        userProfileViewModel.toggleSearchBar()
                    }
                )
            )
        }

        ApplicationBar(
            actions = actions
        )
    }
}
