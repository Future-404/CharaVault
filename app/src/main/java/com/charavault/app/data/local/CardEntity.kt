package com.charavault.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["fileHash"]),
        Index(value = ["semanticHash"]),
        Index(value = ["normalizedJsonHash"])
    ]
)
data class CardEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val creator: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMes: String,
    val systemPrompt: String,
    val tagsJson: String,
    val categorySortOrdersJson: String = "{}",
    val alternateGreetingsJson: String,
    val rawJsonData: String,
    val imagePath: String,
    val fileHash: String = "",
    val normalizedJsonHash: String = "",
    val semanticHash: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
