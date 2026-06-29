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
    val awayLineup: TeamLineup? = null,
    val tableUrl: String? = null,
    val leagueId: String? = null,
    val startTimeMs: Long? = null
)

@Serializable
data class PlayoffRound(
    val roundName: String,
    val matchups: List<PlayoffMatchup>
)

@Serializable
data class PlayoffMatchup(
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val winner: String?
)

@Serializable
data class TableEntry(
    val id: String,
    val name: String,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int
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
