package com.paperapps.paperscores.network.models

data class TeamDetails(
    val id: String,
    val name: String,
    val country: String,
    val primaryLeagueName: String,
    val fixtures: List<MatchDetails>,
    val teamForm: List<TeamForm> = emptyList(),
    val nextMatch: NextMatch? = null,
    val table: List<TableEntry>? = null,
    val squad: List<SquadSection> = emptyList()
)

data class TeamForm(
    val result: String,
    val resultString: String,
    val score: String,
    val tooltipText: String,
    val imageUrl: String
)

data class NextMatch(
    val id: String,
    val opponentName: String,
    val isHome: Boolean,
    val date: String,
    val tournamentName: String
)

data class SquadSection(
    val title: String,
    val members: List<SquadMember>
)

data class SquadMember(
    val id: String,
    val name: String,
    val shirtNumber: Int?,
    val role: String,
    val ccode: String?
)
