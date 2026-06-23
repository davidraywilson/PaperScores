package com.paperapps.paperscores.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.paperapps.paperscores.ui.screens.TodaysGamesScreen
import com.paperapps.paperscores.ui.screens.GameDetailsScreen
import com.paperapps.paperscores.ui.screens.UserProfileScreen
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show TopAppBar for main tabs, GameDetails handles its own or we can hide it.
            // Wait, we can use a generic title or switch based on route.
            val title = when {
                currentRoute == "todays_games" -> "Today's Games"
                currentRoute == "user_profile" -> "Profile & Teams"
                currentRoute?.startsWith("game_details") == true -> "Match Details"
                else -> "PaperScores"
            }

            Column() {
                TopAppBarMMD(
                    title = {
                        when {
                            currentRoute?.startsWith("game_details") == true -> {
                                val viewModel: com.paperapps.paperscores.ui.viewmodel.GameDetailsViewModel? = navBackStackEntry?.let { androidx.lifecycle.viewmodel.compose.viewModel(it) }
                                val matchDetails by (viewModel?.matchDetails ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()

                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val tName = matchDetails?.tournamentName
                                    if (!tName.isNullOrBlank()) {
                                        Text(
                                            text = tName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            else -> {
                                TextMMD(title, fontWeight = FontWeight.Bold)
                            }
                        }

                    },
                    navigationIcon = {
                        if (currentRoute?.startsWith("game_details") == true) {
                            androidx.compose.material3.IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == "todays_games") {
                            val coroutineScope = rememberCoroutineScope()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            androidx.compose.material3.IconButton(onClick = {
                                android.widget.Toast.makeText(context, "Refreshing scores...", android.widget.Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    com.paperapps.paperscores.repository.SoccerRepository.getInstance().refreshTodaysGames()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh"
                                )
                            }
                        } else if (currentRoute?.startsWith("game_details") == true) {
                            val matchId = navBackStackEntry?.arguments?.getString("matchId")
                            if (matchId != null) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val viewModel: com.paperapps.paperscores.ui.viewmodel.GameDetailsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(navBackStackEntry!!)
                                androidx.compose.material3.IconButton(onClick = {
                                    android.widget.Toast.makeText(context, "Refreshing match details...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.loadMatchDetails(matchId)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Refresh"
                                    )
                                }
                            }
                        }
                    }
                )
                HorizontalDividerMMD(thickness = 3.dp)
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute == "todays_games" || currentRoute == "user_profile") {
                Column() {
                    HorizontalDividerMMD(thickness = 1.dp)
                    NavigationBarMMD {
                        NavigationBarItemMMD(
                            icon = { Icon(painter = rememberVectorPainter(Icons.Filled.Home), contentDescription = "Today") },
                            label = { TextMMD("Today") },
                            selected = currentRoute == "todays_games",
                            onClick = {
                                navController.navigate("todays_games") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItemMMD(
                            icon = { Icon(painter = rememberVectorPainter(Icons.Filled.Person), contentDescription = "Profile") },
                            label = { TextMMD("Profile") },
                            selected = currentRoute == "user_profile",
                            onClick = {
                                navController.navigate("user_profile") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "todays_games",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) },
            popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
            popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) },
        ) {
            composable(
                "todays_games",
                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) },
                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) }
            ) {
                TodaysGamesScreen(
                    onGameClick = { matchId ->
                        navController.navigate("game_details/$matchId")
                    }
                )
            }
            composable(
                "game_details/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) },
                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) }
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
                GameDetailsScreen(
                    matchId = matchId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                "user_profile",
                enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) },
                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.snap()) },
                popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.snap()) }
            ) {
                UserProfileScreen()
            }
        }
    }
}
