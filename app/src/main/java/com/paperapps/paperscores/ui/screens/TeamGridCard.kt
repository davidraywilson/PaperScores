package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.paperapps.paperscores.network.models.Team
import com.paperapps.paperscores.network.models.TeamNextMatch
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite

@Composable
fun TeamGridCard(
    team: Team,
    nextMatch: TeamNextMatch?,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(2.dp, PureBlack, RoundedCornerShape(12.dp))
            .background(PureWhite, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = team.imageUrl,
                contentDescription = team.name,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = team.name,
                fontSize = 18.sp,
                color = PureBlack,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (nextMatch != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (nextMatch.isHome) Icons.Default.Home else Icons.Default.FlightTakeoff,
                        contentDescription = if (nextMatch.isHome) "Home" else "Away",
                        modifier = Modifier.size(16.dp),
                        tint = PureBlack
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = nextMatch.opponentName,
                        fontSize = 12.sp,
                        color = PureBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${nextMatch.matchDate}, ${nextMatch.matchTime}",
                    fontSize = 11.sp,
                    color = PureBlack
                )
            }
        }
        
        IconButton(
            onClick = onUnfollow,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Unfollow",
                tint = PureBlack
            )
        }
    }
}
