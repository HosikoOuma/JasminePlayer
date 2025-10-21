package com.example.jasmineplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CustomVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isScrollInProgress by remember { derivedStateOf { listState.isScrollInProgress } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerInput(listState) { detectDragGestures { change, _ ->
                change.consume()
                val trackHeight = size.height.toFloat()
                val dragProgress = (change.position.y / trackHeight).coerceIn(0f, 1f)

                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems > 0) {
                    coroutineScope.launch {
                        val targetIndex = (dragProgress * (totalItems - 1)).toInt()
                        listState.scrollToItem(targetIndex)
                    }
                }
            } }
    ) {
        AnimatedVisibility(
            visible = isScrollInProgress,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0 || layoutInfo.visibleItemsInfo.isEmpty()) return@AnimatedVisibility

            val visibleItemsCount = layoutInfo.visibleItemsInfo.size.toFloat()
            val scrollableItems = (totalItems - visibleItemsCount).coerceAtLeast(1f)

            val scrollProgress = listState.firstVisibleItemIndex.toFloat() / scrollableItems

            val thumbHeight = (maxHeight * (visibleItemsCount / totalItems)).coerceAtLeast(20.dp)
            val thumbOffsetY = (maxHeight - thumbHeight) * scrollProgress

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(thumbHeight)
                    .offset(y = thumbOffsetY)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
    }
}
