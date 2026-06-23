package com.paperapps.paperscores.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.theme.PureBlack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PanoramaHeader(
    pagerState: PagerState,
    titles: List<String>,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    peekPadding: Dp = 0.dp
) {
    Layout(
        modifier = modifier,
        content = {
            titles.forEachIndexed { index, title ->
                val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val distance = abs(index - absolutePosition)
                
                val isSelected = distance < 0.5f
                val weight = if (isSelected) FontWeight.Bold else FontWeight.Light
                val color = if (isSelected) PureBlack else EInkGrey
                
                Text(
                    text = title.lowercase(),
                    fontSize = 40.sp,
                    fontWeight = weight,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { 
            it.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        }
        
        val startPadding = 16.dp.roundToPx()
        val itemSpacing = 24.dp.roundToPx()
        
        var currentX = 0
        val positions = mutableListOf<Int>()
        var maxHeight = 0
        
        placeables.forEachIndexed { index, placeable ->
            val pos = if (index == 0) startPadding else currentX + itemSpacing
            positions.add(pos)
            currentX = pos + placeable.width
            maxHeight = maxOf(maxHeight, placeable.height)
        }
        
        val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
        
        val scrollX = if (absolutePosition < 0) {
            val distance = (positions.getOrNull(1) ?: positions[0]) - positions[0]
            absolutePosition * distance
        } else if (absolutePosition > titles.lastIndex) {
            val distance = positions.last() - (positions.getOrNull(titles.lastIndex - 1) ?: positions.last())
            (positions.last() - startPadding) + (absolutePosition - titles.lastIndex) * distance
        } else {
            val lowerIndex = kotlin.math.floor(absolutePosition).toInt()
            val upperIndex = kotlin.math.ceil(absolutePosition).toInt()
            val fraction = absolutePosition - lowerIndex
            
            val x0 = positions[lowerIndex] - startPadding
            val x1 = positions[upperIndex] - startPadding
            x0 + (x1 - x0) * fraction
        }
        
        layout(constraints.maxWidth, maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val x = positions[index] - scrollX.roundToInt()
                placeable.placeRelative(x, 0)
            }
        }
    }
}




