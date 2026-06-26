package com.paperapps.paperscores.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.theme.PureBlack
import com.paperapps.paperscores.theme.PureWhite

data class AppbarAction(
    val icon: ImageVector,
    val label: String,
    val isLoading: Boolean = false,
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
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
                                .size(40.dp)
                                .clickable(enabled = !action.isLoading) {
                                    action.onClick()
                                    expanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (action.isLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PureBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = PureBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
                    .size(40.dp)
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
            enter = fadeIn(animationSpec = snap()),
            exit = fadeOut(animationSpec = snap())
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


