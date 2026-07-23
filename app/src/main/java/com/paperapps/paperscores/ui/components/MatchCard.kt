package com.paperapps.paperscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite

@Composable
fun MatchCard(match: MatchDetails, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            TeamRow(team = match.homeTeam, score = match.score.home?.toString() ?: "")
            Spacer(modifier = Modifier.height(3.dp))
            TeamRow(team = match.awayTeam, score = match.score.away?.toString() ?: "")
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = match.tournamentName,
                    fontSize = 12.sp,
                    color = PureBlack,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                if (match.status !in listOf("Finished", "Active", "Upcoming") && match.liveTime.isNotBlank() && match.liveTime != match.status) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.End
                    ) {
                        Text(
                            text = match.liveTime,
                            fontSize = 12.sp,
                            color = PureBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = match.status,
                            fontSize = 12.sp,
                            color = PureBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val displayTime = try {
                        val formatterIn = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                            .withZone(java.time.ZoneId.of("UTC"))
                        val zonedDateTime = java.time.ZonedDateTime.parse(match.matchTime, formatterIn)
                        val formatterOut = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy \u2022 h:mm a")
                        zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).format(formatterOut)
                    } catch (e: Exception) {
                        try {
                            val formatterIn = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm z", java.util.Locale.US)
                            val zonedDateTime = java.time.ZonedDateTime.parse(match.matchTime, formatterIn)
                            val formatterOut = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy \u2022 h:mm a")
                            zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).format(formatterOut)
                        } catch (e2: Exception) {
                            match.matchTime
                        }
                    }

                    Text(
                        text = displayTime,
                        fontSize = 12.sp,
                        color = PureBlack,
                        fontWeight = if (match.status !in listOf("Finished", "Active", "Upcoming")) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TeamRow(team: com.paperapps.paperscores.network.models.Team, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://images.fotmob.com/image_resources/logo/teamlogo/${team.id}.png",
            contentDescription = team.name,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        Row(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Box(
                modifier = Modifier
                    .background(PureBlack)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = team.name.uppercase(),
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        Box(
            modifier = Modifier
                .background(PureBlack)
                .defaultMinSize(minWidth = 28.dp)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = score,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
