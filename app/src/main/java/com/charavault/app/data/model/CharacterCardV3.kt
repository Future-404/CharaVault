package com.charavault.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Character Card Spec V3 Unified Data Model
 * Ref: https://github.com/malfoys/character-card-spec-v3
 */
@Serializable
data class CharacterCardV3(
    val spec: String = "chara_card_v3",
    @SerialName("spec_version") val specVersion: String = "3.0",
    val data: CardData = CardData()
)

@Serializable
data class CardData(
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    @SerialName("first_mes") val firstMes: String = "",
    @SerialName("mes_example") val mesExample: String = "",
    val creator: String = "",
    @SerialName("creator_notes") val creatorNotes: String = "",
    @SerialName("system_prompt") val systemPrompt: String = "",
    @SerialName("post_history_instructions") val postHistoryInstructions: String = "",
    @SerialName("alternate_greetings") val alternateGreetings: List<String> = emptyList(),
    @SerialName("group_only_greetings") val groupOnlyGreetings: List<String> = emptyList(),
    @SerialName("character_version") val characterVersion: String = "1.0.0",
    val tags: List<String> = emptyList(),
    @SerialName("character_book") val characterBook: CharacterBook? = null
)

@Serializable
data class CharacterBook(
    val name: String? = null,
    val description: String? = null,
    val entries: List<CharacterBookEntry> = emptyList()
)

@Serializable
data class CharacterBookEntry(
    val id: Int? = null,
    val keys: List<String> = emptyList(),
    @SerialName("secondary_keys") val secondaryKeys: List<String> = emptyList(),
    val comment: String = "",
    val content: String = "",
    val constant: Boolean = false,
    val selective: Boolean = false,
    @SerialName("insertion_order") val insertionOrder: Int = 100,
    val enabled: Boolean = true,
    val position: String = "before_char",
    @SerialName("use_regex") val useRegex: Boolean = false
)
