package com.charavault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charavault.app.data.local.CardEntity
import com.charavault.app.ui.viewmodel.CardGroup
import kotlin.math.roundToInt

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

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

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

        // Horizontal Carousel LazyRow with Long-Press Drag & Drop Support
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            itemsIndexed(currentCards, key = { _, card -> card.id }) { index, card ->
                val isDragging = index == draggingIndex
                val scale by animateFloatAsState(if (isDragging) 1.06f else 1.0f, label = "Scale")

                Box(
                    modifier = Modifier
                        .scale(scale)
                        .offset {
                            if (isDragging) IntOffset(dragOffsetX.roundToInt(), 0) else IntOffset.Zero
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetX = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                    onReorderCards(currentCards.map { it.id })
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x

                                    val itemWidthPx = 360f // Card width + gap in px
                                    val targetIndex = (index + (dragOffsetX / itemWidthPx).roundToInt())
                                        .coerceIn(0, currentCards.size - 1)

                                    if (targetIndex != index && draggingIndex != null) {
                                        val movedItem = currentCards.removeAt(draggingIndex!!)
                                        currentCards.add(targetIndex, movedItem)
                                        draggingIndex = targetIndex
                                        dragOffsetX = 0f
                                    }
                                }
                            )
                        }
                ) {
                    CardItem(
                        card = card,
                        onClick = {
                            if (draggingIndex == null) {
                                onCardClick(card)
                            }
                        }
                    )
                }
            }
        }
    }
}
