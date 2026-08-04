package com.charavault.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val creator: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMes: String,
    val systemPrompt: String,
    val tagsJson: String, // Stored as JSON array string
    val alternateGreetingsJson: String, // Stored as JSON array string
    val rawJsonData: String, // Complete CharacterCardV3 JSON
    val imagePath: String, // Local storage relative/absolute filepath
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
