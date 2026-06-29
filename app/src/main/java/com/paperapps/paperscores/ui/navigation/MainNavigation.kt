package com.paperapps.paperscores.ui.navigation

import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paperapps.paperscores.ui.screens.GameDetailsScreen
import com.paperapps.paperscores.ui.screens.LandingScreen
import com.paperapps.paperscores.ui.screens.TeamDetailsScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "landing",
        enterTransition = { fadeIn(animationSpec = snap()) },
        exitTransition = { fadeOut(animationSpec = snap()) },
        popEnterTransition = { fadeIn(animationSpec = snap()) },
        popExitTransition = { fadeOut(animationSpec = snap()) },
    ) {
        composable(
            "landing",
            enterTransition = { fadeIn(animationSpec = snap()) },
            exitTransition = { fadeOut(animationSpec = snap()) },
            popEnterTransition = { fadeIn(animationSpec = snap()) },
            popExitTransition = { fadeOut(animationSpec = snap()) }
        ) {
            LandingScreen(
                onGameClick = { matchId ->
                    navController.navigate("game_details/$matchId")
                },
                onTeamClick = { teamId ->
                    navController.navigate("team_details/$teamId")
                }
            )
        }
        
        composable(
            "game_details/{matchId}",
            arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "paperscores://game/{matchId}" }),
            enterTransition = { fadeIn(animationSpec = snap()) },
            exitTransition = { fadeOut(animationSpec = snap()) },
            popEnterTransition = { fadeIn(animationSpec = snap()) },
            popExitTransition = { fadeOut(animationSpec = snap()) }
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            GameDetailsScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() },
                onTeamClick = { teamId ->
                    navController.navigate("team_details/$teamId")
                }
            )
        }

        composable(
            "team_details/{teamId}",
            arguments = listOf(navArgument("teamId") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = snap()) },
            exitTransition = { fadeOut(animationSpec = snap()) },
            popEnterTransition = { fadeIn(animationSpec = snap()) },
            popExitTransition = { fadeOut(animationSpec = snap()) }
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
            TeamDetailsScreen(
                teamId = teamId,
                onBackClick = { navController.popBackStack() },
                onGameClick = { matchId ->
                    navController.navigate("game_details/$matchId")
                },
                onTeamClick = { newTeamId ->
                    navController.navigate("team_details/$newTeamId")
                }
            )
        }
    }
}

