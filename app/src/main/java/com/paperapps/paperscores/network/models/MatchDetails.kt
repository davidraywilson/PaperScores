package com.paperapps.paperscores.network.models

import kotlinx.serialization.Serializable

@Serializable
data class MatchEvent(
    val timeStr: String,
    val type: String,
    val nameStr: String
)

@Serializable
data class MatchStat(
    val title: String,
    val homeStat: String,
    val awayStat: String
)

@Serializable
data class LineupPlayer(
    val name: String,
    val shirtNumber: String
)

@Serializable
data class TeamLineup(
    val formation: String,
    val starters: List<LineupPlayer>
)

@Serializable
data class MatchDetails(
    val matchId: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val score: Score,
    val status: String,
    val matchTime: String,
    val liveTime: String = "",
    val tournamentId: String = "",
    val tournamentName: String = "",
    val stadiumName: String = "",
    val events: List<MatchEvent> = emptyList(),
    val stats: List<MatchStat> = emptyList(),
    val homeLineup: TeamLineup? = null,
    val awayLineup: TeamLineup? = null
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    val imageUrl: String
)

@Serializable
data class TeamNextMatch(
    val opponentName: String,
    val isHome: Boolean,
    val matchDate: String,
    val matchTime: String
)

@Serializable
data class Tournament(
    val id: String,
    val name: String,
    val imageUrl: String
)

sealed class SearchResult {
    data class TeamResult(val team: Team) : SearchResult()
    data class TournamentResult(val tournament: Tournament) : SearchResult()
}

@Serializable
data class Score(
    val home: Int?,
    val away: Int?
)
