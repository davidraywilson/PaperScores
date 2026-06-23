package com.paperapps.paperscores.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperapps.paperscores.theme.EInkGrey
import com.paperapps.paperscores.theme.PureBlack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PanoramaHeader(
    pagerState: PagerState,
    titles: List<String>,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.graphicsLayer {
                val currentOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                translationX = -currentOffset * 300f // Parallax scroll speed
            }
        ) {
            titles.forEachIndexed { index, title ->
                val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                val distance = abs(pageOffset)
                
                val isSelected = distance < 0.5f
                val weight = if (isSelected) FontWeight.Bold else FontWeight.Light
                val color = if (isSelected) PureBlack else EInkGrey
                
                Text(
                    text = title.lowercase(),
                    fontSize = 40.sp,
                    fontWeight = weight,
                    color = color,
                    modifier = Modifier
                        .padding(start = if (index == 0) 16.dp else 24.dp, end = if (index == titles.lastIndex) 32.dp else 0.dp)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
