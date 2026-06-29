package com.paperapps.paperscores.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.network.models.TableEntry
import com.paperapps.paperscores.theme.PureBlack

@Composable
fun TableView(table: List<TableEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("Team", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("P", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("W", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("D", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("L", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("G/C", modifier = Modifier.width(42.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
                Text("Pts", modifier = Modifier.width(28.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
            }
            DashedDivider()
        }

        items(table.size) { index ->
            val entry = table[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", modifier = Modifier.width(28.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureBlack)
                Text(entry.name, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.played}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = PureBlack)
                Text("${entry.wins}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = PureBlack)
                Text("${entry.draws}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = PureBlack)
                Text("${entry.losses}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = PureBlack)
                Text("${entry.goalsFor}/${entry.goalsAgainst}", modifier = Modifier.width(42.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = PureBlack)
                Text("${entry.points}", modifier = Modifier.width(28.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureBlack)
            }
            DashedDivider()
        }
    }
}
