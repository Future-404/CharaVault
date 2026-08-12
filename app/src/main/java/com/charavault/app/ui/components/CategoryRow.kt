package com.charavault.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.charavault.app.data.local.CardEntity
import com.charavault.app.ui.viewmodel.CardGroup
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_AUTO_SCROLL_SPEED_PX_PER_SECOND = 120f
private const val MAX_AUTO_SCROLL_SPEED_PX_PER_SECOND = 1500f
private const val MAX_FRAME_DURATION_NANOS = 50_000_000L
private const val DRAG_LONG_PRESS_TIMEOUT_MILLIS = 350L
private const val DRAG_TOUCH_SLOP_MULTIPLIER = 0.65f

internal fun calculateHorizontalAutoScrollSpeed(
    pointerX: Float,
    viewportWidth: Float,
    edgeThreshold: Float
): Float {
    if (viewportWidth <= 0f || edgeThreshold <= 0f) return 0f

    val effectiveThreshold = edgeThreshold.coerceAtMost(viewportWidth / 2f)
    val startEdgeProgress = ((effectiveThreshold - pointerX) / effectiveThreshold).coerceIn(0f, 1f)
    if (startEdgeProgress > 0f) {
        return -(MIN_AUTO_SCROLL_SPEED_PX_PER_SECOND +
            (MAX_AUTO_SCROLL_SPEED_PX_PER_SECOND - MIN_AUTO_SCROLL_SPEED_PX_PER_SECOND) * startEdgeProgress)
    }

    val endEdgeStart = viewportWidth - effectiveThreshold
    val endEdgeProgress = ((pointerX - endEdgeStart) / effectiveThreshold).coerceIn(0f, 1f)
    if (endEdgeProgress > 0f) {
        return MIN_AUTO_SCROLL_SPEED_PX_PER_SECOND +
            (MAX_AUTO_SCROLL_SPEED_PX_PER_SECOND - MIN_AUTO_SCROLL_SPEED_PX_PER_SECOND) * endEdgeProgress
    }

    return 0f
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryRow(
    group: CardGroup,
    onCardClick: (CardEntity) -> Unit,
    onReorderCards: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCards = remember(group.title) {
        mutableStateListOf<CardEntity>().apply { addAll(group.cards) }
    }
    var draggedCardId by remember { mutableStateOf<String?>(null) }

    // Keep internal list in sync with external group.cards when not dragging
    LaunchedEffect(group.cards) {
        if (draggedCardId == null) {
            currentCards.clear()
            currentCards.addAll(group.cards)
        }
    }

    val lazyListState = rememberLazyListState()
    val edgeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

    var rowWidthPx by remember { mutableIntStateOf(0) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
    var draggedPointerX by remember { mutableFloatStateOf(0f) }
    var draggedPointerOffset by remember { mutableFloatStateOf(0f) }
    var draggedCardBaseLeft by remember { mutableFloatStateOf(0f) }
    var draggedCardWidth by remember { mutableIntStateOf(0) }
    var dragDirection by remember { mutableFloatStateOf(0f) }

    val currentReorderCardsLambda by rememberUpdatedState(onReorderCards)
    val latestGroupCards by rememberUpdatedState(group.cards)
    val defaultViewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(defaultViewConfiguration) {
        object : ViewConfiguration by defaultViewConfiguration {
            override val longPressTimeoutMillis: Long
                get() = DRAG_LONG_PRESS_TIMEOUT_MILLIS
            override val touchSlop: Float
                get() = defaultViewConfiguration.touchSlop * DRAG_TOUCH_SLOP_MULTIPLIER
        }
    }

    fun cardIndexForKey(key: Any?, suggestedIndex: Int): Int {
        val cardId = key as? String ?: return -1
        return if (currentCards.getOrNull(suggestedIndex)?.id == cardId) {
            suggestedIndex
        } else {
            currentCards.indexOfFirst { it.id == cardId }
        }
    }

    fun moveDraggedCardIfNeeded() {
        val draggedId = draggedCardId ?: return
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggedCenter = draggedPointerX - draggedPointerOffset + draggedCardWidth / 2f
        val currentIndex = currentCards.indexOfFirst { it.id == draggedId }
        if (currentIndex !in currentCards.indices) return

        val targetItem = visibleItems.firstOrNull { item ->
            item.key != draggedId &&
                draggedCenter >= item.offset &&
                draggedCenter <= item.offset + item.size
        } ?: return
        val targetIndex = cardIndexForKey(targetItem.key, targetItem.index)
        if (currentIndex !in currentCards.indices || targetIndex !in currentCards.indices) return
        if (dragDirection < 0f && targetIndex >= currentIndex) return
        if (dragDirection > 0f && targetIndex <= currentIndex) return

        val itemToMove = currentCards.removeAt(currentIndex)
        currentCards.add(targetIndex, itemToMove)
    }

    LaunchedEffect(draggedCardId) {
        if (draggedCardId != null) {
            var previousFrameTimeNanos = 0L
            while (isActive && draggedCardId != null) {
                val frameTimeNanos = withFrameNanos { it }
                val elapsedNanos = if (previousFrameTimeNanos == 0L) {
                    1_000_000_000L / 60L
                } else {
                    (frameTimeNanos - previousFrameTimeNanos).coerceIn(0L, MAX_FRAME_DURATION_NANOS)
                }
                previousFrameTimeNanos = frameTimeNanos

                val speed = autoScrollSpeed
                if (speed == 0f) continue

                val requestedScroll = speed * (elapsedNanos / 1_000_000_000f)
                val consumedScroll = lazyListState.scrollBy(requestedScroll)
                if (consumedScroll != 0f) {
                    dragDirection = if (speed < 0f) -1f else 1f
                    moveDraggedCardIfNeeded()
                }
            }
        }
    }

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
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${group.cards.size} 张",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Smooth, Follow-finger Horizontal Carousel LazyRow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowWidthPx = it.width }
        ) {
            CompositionLocalProvider(LocalViewConfiguration provides dragViewConfiguration) {
                LazyRow(
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .pointerInput(edgeThresholdPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                                val item = visibleItems.firstOrNull { visibleItem ->
                                    offset.x >= visibleItem.offset && offset.x <= (visibleItem.offset + visibleItem.size)
                                }
                                item?.let {
                                    val cardId = it.key as? String
                                    if (cardId != null) {
                                        draggedCardId = cardId
                                        draggedPointerX = offset.x
                                        draggedPointerOffset = offset.x - it.offset
                                        draggedCardBaseLeft = it.offset.toFloat()
                                        draggedCardWidth = it.size
                                        dragDirection = 0f
                                        autoScrollSpeed = 0f
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (draggedCardId == null) return@detectDragGesturesAfterLongPress

                                val pointerDelta = change.position.x - draggedPointerX
                                if (abs(pointerDelta) > 0.5f) {
                                    dragDirection = if (pointerDelta < 0f) -1f else 1f
                                }
                                draggedPointerX = change.position.x
                                autoScrollSpeed = calculateHorizontalAutoScrollSpeed(
                                    pointerX = change.position.x,
                                    viewportWidth = rowWidthPx.toFloat(),
                                    edgeThreshold = edgeThresholdPx
                                )
                                if (autoScrollSpeed != 0f) {
                                    dragDirection = if (autoScrollSpeed < 0f) -1f else 1f
                                }

                                moveDraggedCardIfNeeded()
                            },
                            onDragEnd = {
                                val finalIds = currentCards.map { it.id }
                                draggedCardId = null
                                autoScrollSpeed = 0f
                                draggedPointerX = 0f
                                draggedPointerOffset = 0f
                                draggedCardBaseLeft = 0f
                                draggedCardWidth = 0
                                dragDirection = 0f
                                currentReorderCardsLambda(finalIds)
                            },
                            onDragCancel = {
                                draggedCardId = null
                                autoScrollSpeed = 0f
                                draggedPointerX = 0f
                                draggedPointerOffset = 0f
                                draggedCardBaseLeft = 0f
                                draggedCardWidth = 0
                                dragDirection = 0f
                                currentCards.clear()
                                currentCards.addAll(latestGroupCards)
                            }
                        )
                    }
                ) {
                items(
                    items = currentCards,
                    key = { card -> card.id }
                ) { card ->
                    val isDragging = card.id == draggedCardId
                    val placementModifier = if (draggedCardId == null) {
                        Modifier.animateItemPlacement()
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = placementModifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { alpha = if (isDragging) 0f else 1f }
                    ) {
                        CardItem(
                            card = card,
                            onClick = {
                                if (draggedCardId == null) {
                                    onCardClick(card)
                                }
                            }
                        )
                    }
                }
            }

                val draggedCard = currentCards.firstOrNull { it.id == draggedCardId }
                if (draggedCard != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .offset { IntOffset(draggedCardBaseLeft.roundToInt(), 0) }
                            .zIndex(10f)
                            .graphicsLayer {
                                translationX = draggedPointerX - draggedPointerOffset - draggedCardBaseLeft
                                scaleX = 1.08f
                                scaleY = 1.08f
                            }
                    ) {
                        CardItem(card = draggedCard, onClick = {})
                    }
                }
            }
        }
    }
}
