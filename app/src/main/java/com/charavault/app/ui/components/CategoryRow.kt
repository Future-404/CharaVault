package com.charavault.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.charavault.app.data.local.CardEntity
import com.charavault.app.ui.viewmodel.CardGroup

@Composable
fun CategoryRow(
    group: CardGroup,
    onCardClick: (CardEntity) -> Unit,
    onReorderCards: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCards = remember(group.cards) {
        mutableStateListOf<CardEntity>().apply { addAll(group.cards) }
    }

    val lazyListState = rememberLazyListState()

    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemOffset by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        // Clean Minimal Header Title & Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${group.cards.size} 张",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Smooth, Follow-finger Horizontal Carousel LazyRow
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(top = 8.dp)
                .pointerInput(currentCards) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                            val item = visibleItems.firstOrNull { visibleItem ->
                                offset.x >= visibleItem.offset && offset.x <= (visibleItem.offset + visibleItem.size)
                            }
                            item?.let {
                                draggingItemIndex = it.index
                                draggingItemOffset = 0f
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val currentIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                            draggingItemOffset += dragAmount.x

                            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                            val currentVisibleItem = visibleItems.firstOrNull { it.index == currentIndex }
                                ?: return@detectDragGesturesAfterLongPress

                            val draggedCenter = currentVisibleItem.offset + (currentVisibleItem.size / 2f) + draggingItemOffset

                            // Check collision with adjacent items
                            val targetItem = visibleItems.firstOrNull { item ->
                                item.index != currentIndex &&
                                    draggedCenter >= item.offset &&
                                    draggedCenter <= (item.offset + item.size)
                            }

                            if (targetItem != null) {
                                val targetIndex = targetItem.index
                                if (targetIndex in 0 until currentCards.size) {
                                    val itemToMove = currentCards.removeAt(currentIndex)
                                    currentCards.add(targetIndex, itemToMove)
                                    draggingItemIndex = targetIndex
                                    draggingItemOffset += (currentVisibleItem.offset - targetItem.offset)
                                }
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = null
                            draggingItemOffset = 0f
                            onReorderCards(currentCards.map { it.id })
                        },
                        onDragCancel = {
                            draggingItemIndex = null
                            draggingItemOffset = 0f
                        }
                    )
                }
        ) {
            itemsIndexed(currentCards, key = { _, card -> card.id }) { index, card ->
                val isDragging = index == draggingItemIndex
                val zIndex = if (isDragging) 10f else 1f
                val scale = if (isDragging) 1.08f else 1.0f

                Box(
                    modifier = Modifier
                        .zIndex(zIndex)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = if (isDragging) draggingItemOffset else 0f
                        }
                ) {
                    CardItem(
                        card = card,
                        onClick = {
                            if (draggingItemIndex == null) {
                                onCardClick(card)
                            }
                        }
                    )
                }
            }
        }
    }
}
