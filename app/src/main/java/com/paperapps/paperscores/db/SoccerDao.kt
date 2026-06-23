package com.paperapps.paperscores.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoccerDao {
    @Query("SELECT * FROM followed_teams")
    fun getFollowedTeamsFlow(): Flow<List<FollowedTeamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTeam(team: FollowedTeamEntity)

    @Query("DELETE FROM followed_teams WHERE id = :teamId")
    fun deleteTeam(teamId: String)

    @Query("SELECT * FROM followed_tournaments")
    fun getFollowedTournamentsFlow(): Flow<List<FollowedTournamentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTournament(tournament: FollowedTournamentEntity)

    @Query("DELETE FROM followed_tournaments WHERE id = :tournamentId")
    fun deleteTournament(tournamentId: String)
}
