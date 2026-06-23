package com.paperapps.paperscores.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paperapps.paperscores.network.models.Team

@Entity(tableName = "followed_teams")
data class FollowedTeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String
) {
    fun toTeam() = Team(id = id, name = name, imageUrl = imageUrl)
    
    companion object {
        fun fromTeam(team: Team) = FollowedTeamEntity(
            id = team.id,
            name = team.name,
            imageUrl = team.imageUrl
        )
    }
}
