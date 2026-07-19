package com.paperapps.paperscores.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paperapps.paperscores.ui.viewmodel.SettingsViewModel

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.theme.PureBlack

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isRemindersEnabled by viewModel.isMatchRemindersEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp)
    ) {
        Text(
            text = "notifications",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PureBlack,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Match Reminders",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Receive notifications 15 minutes before your followed games start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = isRemindersEnabled,
                onCheckedChange = { viewModel.setMatchRemindersEnabled(it) }
            )
        }
    }
}
