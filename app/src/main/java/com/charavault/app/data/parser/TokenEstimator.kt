package com.charavault.app.data.parser

import com.charavault.app.data.model.CharacterCardV3
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingType
import java.util.LinkedHashMap

data class CardTokenStats(
    val totalTokens: Int,
    val profileTokens: Int,
    val greetingTokens: Int,
    val advancedTokens: Int,
    val lorebookTokens: Int,
    val enabledLorebookCount: Int,
    val totalLorebookCount: Int
)

object TokenEstimator {

    private const val MAX_CACHED_STATS = 32

    private val encodingRegistry by lazy { Encodings.newLazyEncodingRegistry() }
    private val encoding: Encoding? by lazy {
        try {
            encodingRegistry.getEncoding(EncodingType.CL100K_BASE)
        } catch (e: Exception) {
            null
        }
    }

    private val cacheLock = Any()
    private val cachedStats = object : LinkedHashMap<String, CardTokenStats>(MAX_CACHED_STATS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CardTokenStats>?): Boolean {
            return size > MAX_CACHED_STATS
        }
    }

    /**
     * Count exact BPE Tokens using official Tiktoken cl100k_base (GPT-4 / Claude / Llama)
     */
    fun countTokens(text: String): Int {
        if (text.isBlank()) return 0
        return try {
            encoding?.countTokens(text) ?: fallbackEstimate(text)
        } catch (e: Exception) {
            fallbackEstimate(text)
        }
    }

    /**
     * Calculate comprehensive token statistics breakdown for a CharacterCardV3
     */
    fun calculateCardStats(cardV3: CharacterCardV3?): CardTokenStats {
        if (cardV3 == null) return CardTokenStats(0, 0, 0, 0, 0, 0, 0)
        val data = cardV3.data

        val profileText = "${data.name}\n${data.creator}\n${data.description}\n${data.personality}\n${data.scenario}"
        val profileTokens = countTokens(profileText)

        val greetingText = "${data.firstMes}\n" + data.alternateGreetings.joinToString("\n")
        val greetingTokens = countTokens(greetingText)

        val advancedText = "${data.systemPrompt}\n${data.postHistoryInstructions}\n${data.creatorNotes}"
        val advancedTokens = countTokens(advancedText)

        val entries = data.characterBook?.entries ?: emptyList()
        val enabledEntries = entries.filter { it.enabled }
        val lorebookText = enabledEntries.joinToString("\n") { entry ->
            "${entry.comment} " + entry.keys.joinToString(" ") + " " + entry.secondaryKeys.joinToString(" ") + "\n" + entry.content
        }
        val lorebookTokens = countTokens(lorebookText)

        val totalTokens = profileTokens + greetingTokens + advancedTokens + lorebookTokens

        return CardTokenStats(
            totalTokens = totalTokens,
            profileTokens = profileTokens,
            greetingTokens = greetingTokens,
            advancedTokens = advancedTokens,
            lorebookTokens = lorebookTokens,
            enabledLorebookCount = enabledEntries.size,
            totalLorebookCount = entries.size
        )
    }

    /**
     * Return cached stats for a stable card/content key, calculating them only on a cache miss.
     * The cache is deliberately bounded because card text and lorebooks can be large.
     */
    fun calculateCardStatsCached(cacheKey: String, cardV3: CharacterCardV3?): CardTokenStats {
        if (cardV3 == null) return CardTokenStats(0, 0, 0, 0, 0, 0, 0)

        synchronized(cacheLock) {
            cachedStats[cacheKey]?.let { return it }
        }

        val calculated = calculateCardStats(cardV3)
        synchronized(cacheLock) {
            val existing = cachedStats[cacheKey]
            if (existing != null) return existing
            cachedStats[cacheKey] = calculated
            return calculated
        }
    }

    private fun fallbackEstimate(text: String): Int {
        var tokens = 0.0
        for (char in text) {
            when {
                char.code in 0x4E00..0x9FFF || char.code in 0x3000..0x303F || char.code in 0xFF00..0xFFEF -> tokens += 0.75
                char.code in 0x0020..0x007E -> tokens += 0.3
                else -> tokens += 1.0
            }
        }
        return kotlin.math.ceil(tokens).toInt()
    }
}
