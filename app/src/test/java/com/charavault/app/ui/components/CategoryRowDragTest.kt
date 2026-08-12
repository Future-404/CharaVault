package com.charavault.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRowDragTest {

    @Test
    fun autoScrollSpeed_isZeroAwayFromEdges() {
        val speed = calculateHorizontalAutoScrollSpeed(
            pointerX = 200f,
            viewportWidth = 400f,
            edgeThreshold = 64f
        )

        assertEquals(0f, speed, 0.001f)
    }

    @Test
    fun autoScrollSpeed_isNegativeAndAcceleratesTowardLeftEdge() {
        val nearThreshold = calculateHorizontalAutoScrollSpeed(63f, 400f, 64f)
        val atEdge = calculateHorizontalAutoScrollSpeed(0f, 400f, 64f)

        assertTrue(nearThreshold < 0f)
        assertTrue(atEdge < nearThreshold)
    }

    @Test
    fun autoScrollSpeed_isPositiveAndAcceleratesTowardRightEdge() {
        val nearThreshold = calculateHorizontalAutoScrollSpeed(337f, 400f, 64f)
        val atEdge = calculateHorizontalAutoScrollSpeed(400f, 400f, 64f)

        assertTrue(nearThreshold > 0f)
        assertTrue(atEdge > nearThreshold)
    }

    @Test
    fun autoScrollSpeed_clampsOutsideViewport() {
        val leftEdge = calculateHorizontalAutoScrollSpeed(0f, 400f, 64f)
        val beyondLeft = calculateHorizontalAutoScrollSpeed(-100f, 400f, 64f)
        val rightEdge = calculateHorizontalAutoScrollSpeed(400f, 400f, 64f)
        val beyondRight = calculateHorizontalAutoScrollSpeed(500f, 400f, 64f)

        assertEquals(leftEdge, beyondLeft, 0.001f)
        assertEquals(rightEdge, beyondRight, 0.001f)
    }
}
