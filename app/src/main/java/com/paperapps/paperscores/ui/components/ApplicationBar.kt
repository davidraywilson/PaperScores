package com.paperapps.paperscores.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite

data class AppbarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

data class AppbarMenuItem(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun ApplicationBar(
    actions: List<AppbarAction>,
    menuItems: List<AppbarMenuItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureWhite)
            .clickable(enabled = !expanded) { expanded = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row {
                actions.take(4).forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    action.onClick()
                                    expanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = PureBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = action.label.lowercase(),
                                fontSize = 12.sp,
                                color = PureBlack,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "...",
                    fontSize = 24.sp,
                    color = PureBlack,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                menuItems.forEach { menuItem ->
                    Text(
                        text = menuItem.label.lowercase(),
                        fontSize = 20.sp,
                        color = PureBlack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                menuItem.onClick()
                                expanded = false
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}
