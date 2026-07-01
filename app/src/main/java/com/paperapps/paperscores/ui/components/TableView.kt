package com.paperapps.paperscores.ui.components

import androidx.compose.runtime.Composable
import com.paperapps.paperscores.network.models.TableEntry
import com.paperapps.paperui.components.PaperUITableEntry
import com.paperapps.paperui.components.TableView as PaperUITableView

/**
 * App-level adapter for [PaperUITableView].
 *
 * Converts the FotMob-specific [TableEntry] network model into the library's
 * generic [PaperUITableEntry] and delegates rendering to the :paperui component.
 */
@Composable
fun TableView(table: List<TableEntry>) {
    val entries = table.map { entry ->
        PaperUITableEntry(
            name = entry.name,
            played = entry.played,
            wins = entry.wins,
            draws = entry.draws,
            losses = entry.losses,
            goalsFor = entry.goalsFor,
            goalsAgainst = entry.goalsAgainst,
            points = entry.points,
        )
    }
    PaperUITableView(table = entries)
}
