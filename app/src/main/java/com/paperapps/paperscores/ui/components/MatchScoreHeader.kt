package com.paperapps.paperscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite

@Composable
fun MatchScoreHeader(
    match: MatchDetails,
    onTeamClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(end=16.dp)) {
        val dateFormatted = try {
            val formatterIn = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy, HH:mm z", java.util.Locale.US)
            val zonedDateTime = java.time.ZonedDateTime.parse(match.matchTime, formatterIn)
            val formatterOut = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            zonedDateTime.format(formatterOut)
        } catch (e: Exception) {
            match.matchTime.split(",").take(3).joinToString(",").trim()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            if (match.status == "Active") {
                Text(
                    text = match.liveTime.ifBlank { "LIVE" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            } else if (match.status == "Finished") {
                Text(
                    text = "FT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EInkGrey
                )
            } else if (match.status != "Upcoming") {
                Text(
                    text = match.status,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            }
        }

        if (match.stadiumName.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(match.stadiumName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PureBlack)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

        // Home Team Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clickable { onTeamClick(match.homeTeam.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (match.homeTeam.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = match.homeTeam.imageUrl,
                    contentDescription = match.homeTeam.name,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                    colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                )
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            Row(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .background(PureBlack)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(match.homeTeam.name.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (match.score.home != null) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .background(PureBlack)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${match.score.home}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }

        // Away Team Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTeamClick(match.awayTeam.id) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (match.awayTeam.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = match.awayTeam.imageUrl,
                    contentDescription = match.awayTeam.name,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                    colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
                )
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }

            Row(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Box(
                    modifier = Modifier
                        .background(PureBlack)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(match.awayTeam.name.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (match.score.away != null) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .background(PureBlack)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${match.score.away}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }
        }
    }
}
