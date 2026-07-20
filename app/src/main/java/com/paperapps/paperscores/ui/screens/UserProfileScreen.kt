package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import com.paperapps.paperui.components.PaperLazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.network.models.SearchResult
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.ui.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    onTeamClick: (String) -> Unit,
    viewModel: UserProfileViewModel = viewModel()
) {
    val followedTeams by viewModel.followedTeams.collectAsState()
    val followedTournaments by viewModel.followedTournaments.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isSearchBarVisible by viewModel.isSearchBarVisible.collectAsState()
    val teamFixtures by viewModel.teamFixtures.collectAsState()
    var teamSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(isSearchBarVisible) {
        if (!isSearchBarVisible) {
            teamSearchQuery = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PaperLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp),
            // TeamGridCard grows when nextMatch data arrives (null → loaded),
            // so reset the height cache whenever teamFixtures updates.
            refreshKey = teamFixtures,
        ) {
            if (isSearchBarVisible) {
                item {
                    OutlinedTextField(
                        value = teamSearchQuery,
                        onValueChange = { 
                            teamSearchQuery = it 
                            viewModel.performSearch(it)
                        },
                        placeholder = { Text("search for a team or tournament") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            focusedIndicatorColor = PureBlack,
                            unfocusedIndicatorColor = PureBlack,
                            cursorColor = PureBlack
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isSearching) {
                    item {
                        Text("searching...", fontSize = 14.sp, color = EInkGrey)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (teamSearchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                    items(searchResults) { result ->
                        val name = when (result) {
                            is SearchResult.TeamResult -> result.team.name
                            is SearchResult.TournamentResult -> result.tournament.name
                        }
                        val resultType = when (result) {
                            is SearchResult.TeamResult -> "team"
                            is SearchResult.TournamentResult -> "tournament"
                        }
                        val isFollowed = when (result) {
                            is SearchResult.TeamResult -> followedTeams.any { it.id == result.team.id }
                            is SearchResult.TournamentResult -> followedTournaments.any { it.id == result.tournament.id }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable {
                                    if (result is SearchResult.TeamResult) {
                                        onTeamClick(result.team.id)
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    name,
                                    fontSize = 16.sp,
                                    color = PureBlack,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(resultType, fontSize = 12.sp, color = PureBlack)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (isFollowed) {
                                                when (result) {
                                                    is SearchResult.TeamResult -> viewModel.unfollowTeam(result.team.id)
                                                    is SearchResult.TournamentResult -> viewModel.unfollowTournament(result.tournament.id)
                                                }
                                            } else {
                                                viewModel.follow(result)
                                            }
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = if (isFollowed) "Unfollow" else "Follow",
                                        tint = PureBlack,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (teamSearchQuery.isNotBlank() && !isSearching) {
                item {
                    Text("no results found for \"$teamSearchQuery\"", fontSize = 14.sp, color = PureBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (!isSearchBarVisible) {
                item {
                    Text("followed teams", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(followedTeams.chunked(2)) { rowTeams ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (team in rowTeams) {
                            TeamGridCard(
                                team = team,
                                nextMatch = teamFixtures[team.id],
                                onUnfollow = { viewModel.unfollowTeam(team.id) },
                                modifier = Modifier.weight(1f).clickable { onTeamClick(team.id) }
                            )
                        }
                        if (rowTeams.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("followed tournaments", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(followedTournaments) { tournament ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tournament.name, fontSize = 16.sp, color = PureBlack)
                        Text(
                            text = "unfollow",
                            color = PureBlack,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.unfollowTournament(tournament.id) }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

