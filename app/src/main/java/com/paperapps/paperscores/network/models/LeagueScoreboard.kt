package com.paperapps.paperscores.network.models

import kotlinx.serialization.Serializable

@Serializable
data class LeagueScoreboard(
    val leagueId: String,
    val leagueName: String,
    val matches: List<MatchDetails>
)
