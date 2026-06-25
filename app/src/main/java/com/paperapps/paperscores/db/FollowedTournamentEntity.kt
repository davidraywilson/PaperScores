package com.paperapps.paperscores.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paperapps.paperscores.network.models.Tournament

@Entity(tableName = "followed_tournaments")
data class FollowedTournamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String
) {
    fun toTournament() = Tournament(
        id = id, 
        name = name, 
        imageUrl = imageUrl.ifEmpty { "https://images.fotmob.com/image_resources/logo/leaguelogo/${id}.png" }
    )
    
    companion object {
        fun fromTournament(tournament: Tournament) = FollowedTournamentEntity(
            id = tournament.id,
            name = tournament.name,
            imageUrl = tournament.imageUrl
        )
    }
}
