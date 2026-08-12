package com.charavault.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class CardOrderTest {

    @Test
    fun reorderCategory_preservesOtherCategoryOrders() {
        val result = withCategoryOrder(
            currentOrders = mapOf("冒险" to 4, "女性" to 1),
            category = "冒险",
            order = 0
        )

        assertEquals(mapOf("冒险" to 0, "女性" to 1), result)
    }
}
