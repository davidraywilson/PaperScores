package com.paperapps.paperscores.network.models

data class TournamentDetails(
    val id: String,
    val name: String,
    val table: List<TableEntry> = emptyList(),
    val fixtures: List<MatchDetails> = emptyList(),
    val overviewMatches: List<MatchDetails> = emptyList() // recent or upcoming
)
