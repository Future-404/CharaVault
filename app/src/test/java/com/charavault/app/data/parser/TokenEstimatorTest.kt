package com.charavault.app.data.parser

import com.charavault.app.data.model.CardData
import com.charavault.app.data.model.CharacterBook
import com.charavault.app.data.model.CharacterBookEntry
import com.charavault.app.data.model.CharacterCardV3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenEstimatorTest {

    @Test
    fun nullCardReturnsEmptyStats() {
        assertEquals(CardTokenStats(0, 0, 0, 0, 0, 0, 0), TokenEstimator.calculateCardStats(null))
    }

    @Test
    fun disabledLorebookEntriesAreExcludedFromTokenCount() {
        val card = CharacterCardV3(
            data = CardData(
                name = "Aster",
                description = "A short profile.",
                firstMes = "Hello",
                characterBook = CharacterBook(
                    entries = listOf(
                        CharacterBookEntry(comment = "enabled", content = "Included text", enabled = true),
                        CharacterBookEntry(comment = "disabled", content = "Ignored text", enabled = false)
                    )
                )
            )
        )

        val stats = TokenEstimator.calculateCardStats(card)

        assertEquals(2, stats.totalLorebookCount)
        assertEquals(1, stats.enabledLorebookCount)
        assertTrue(stats.profileTokens > 0)
        assertTrue(stats.greetingTokens > 0)
        assertTrue(stats.lorebookTokens > 0)
        assertEquals(
            stats.profileTokens + stats.greetingTokens + stats.advancedTokens + stats.lorebookTokens,
            stats.totalTokens
        )
    }

    @Test
    fun cachedStatsMatchDirectCalculation() {
        val card = CharacterCardV3(data = CardData(description = "Cached profile"))

        val direct = TokenEstimator.calculateCardStats(card)
        val cached = TokenEstimator.calculateCardStatsCached("token-estimator-test", card)

        assertEquals(direct, cached)
    }
}
