package com.paperapps.paperscores.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paperapps.paperscores.repository.SoccerRepository
import com.paperapps.paperscores.ui.components.AppbarAction
import com.paperapps.paperscores.ui.components.ApplicationBar
import com.paperapps.paperscores.ui.components.PanoramaHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandingScreen(
    onGameClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(48.dp)) // Top padding

        PanoramaHeader(
            pagerState = pagerState,
            titles = listOf("games", "profile"),
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth(),
            peekPadding = 48.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = 48.dp)
        ) { page ->
            when (page) {
                0 -> TodaysGamesScreen(onGameClick = onGameClick)
                1 -> UserProfileScreen()
            }
        }

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.Filled.Refresh,
                    label = "Refresh",
                    onClick = {
                        Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            SoccerRepository.getInstance().refreshTodaysGames()
                        }
                    }
                )
            )
        )
    }
}
