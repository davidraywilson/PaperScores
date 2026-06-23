package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.ui.viewmodel.UserProfileViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import androidx.compose.material3.OutlinedTextField
import com.paperapps.paperscores.network.models.Team
import com.paperapps.paperscores.network.models.Tournament
import com.paperapps.paperscores.network.models.SearchResult

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = viewModel()
) {
    val followedTeams by viewModel.followedTeams.collectAsState()
    val followedTournaments by viewModel.followedTournaments.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    var teamSearchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumnMMD(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = teamSearchQuery,
                    onValueChange = { 
                        teamSearchQuery = it 
                        viewModel.performSearch(it)
                    },
                    placeholder = { TextMMD("Search for a team or tournament") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isSearching) {
                item {
                    TextMMD("Searching...", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (teamSearchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                items(searchResults) { result ->
                    val name = when (result) {
                        is SearchResult.TeamResult -> "${result.team.name} (Team)"
                        is SearchResult.TournamentResult -> "${result.tournament.name} (Tournament)"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextMMD(name, fontSize = 16.sp)
                        ButtonMMD(
                            onClick = { 
                                viewModel.follow(result) 
                                viewModel.clearSearch()
                                teamSearchQuery = ""
                            },
                        ) {
                            TextMMD("Follow")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDividerMMD(thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (teamSearchQuery.isNotBlank() && !isSearching) {
                item {
                    TextMMD("No results found for \"$teamSearchQuery\"", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                TextMMD("Followed Teams", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(followedTeams) { team ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextMMD(team.name, fontSize = 16.sp)
                    ButtonMMD(
                        onClick = { viewModel.unfollowTeam(team.id) }
                    ) {
                        TextMMD("Unfollow")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                TextMMD("Followed Tournaments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(followedTournaments) { tournament ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextMMD(tournament.name, fontSize = 16.sp)
                    ButtonMMD(
                        onClick = { viewModel.unfollowTournament(tournament.id) }
                    ) {
                        TextMMD("Unfollow")
                    }
                }
            }
        }
    }
}
