package com.paperapps.paperscores.theme

// Re-export the PaperUI theme under the legacy name so all existing call sites
// in this module continue to compile without changes.
import androidx.compose.runtime.Composable
import com.paperapps.paperui.theme.PaperUITheme

// Re-export colors so any remaining imports in :app still resolve.
@Suppress("UnusedImport")
val PureBlack  get() = com.paperapps.paperui.theme.PureBlack
val PureWhite  get() = com.paperapps.paperui.theme.PureWhite
val EInkGrey   get() = com.paperapps.paperui.theme.EInkGrey
val EInkDarkGrey get() = com.paperapps.paperui.theme.EInkDarkGrey

/**
 * Thin wrapper kept for backward compatibility inside the :app module.
 * All new code should use [PaperUITheme] from the :paperui library directly.
 */
@Composable
fun SoccerScoresTheme(content: @Composable () -> Unit) = PaperUITheme(content)
