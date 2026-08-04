package com.charavault.app.data.repository

import android.content.Context
import com.charavault.app.data.local.AppDatabase
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CardData
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.data.parser.PngChunkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class CardRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val cardDao = db.cardDao()
    private val cardsDir = File(context.getExternalFilesDir(null), "cards").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }

    fun getAllCards(): Flow<List<CardEntity>> = cardDao.getAllCardsFlow()

    suspend fun importCardStream(
        inputStream: InputStream,
        originalFileName: String?,
        selectedTags: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            val cardV3 = PngChunkUtils.extractCardFromJsonPng(ByteArrayInputStream(bytes))
                ?: createDefaultCardV3(originalFileName ?: "新角色卡")

            val id = UUID.randomUUID().toString().take(8)
            val destFile = File(cardsDir, "chara_$id.png")

            // Determine effective tags
            val finalTags = if (selectedTags.isNotEmpty()) {
                selectedTags
            } else if (cardV3.data.tags.isNotEmpty()) {
                cardV3.data.tags
            } else {
                listOf("未分类")
            }

            val updatedV3 = cardV3.copy(data = cardV3.data.copy(tags = finalTags))
            val jsonStr = json.encodeToString(updatedV3)

            val finalPngBytes = PngChunkUtils.injectJsonIntoPng(bytes, jsonStr)
            destFile.writeBytes(finalPngBytes)

            val name = updatedV3.data.name.ifBlank { originalFileName?.substringBeforeLast(".") ?: "未命名角色" }
            val creator = updatedV3.data.creator.ifBlank { "未知作者" }

            val entity = CardEntity(
                id = id,
                name = name,
                creator = creator,
                description = updatedV3.data.description,
                personality = updatedV3.data.personality,
                scenario = updatedV3.data.scenario,
                firstMes = updatedV3.data.firstMes,
                systemPrompt = updatedV3.data.systemPrompt,
                tagsJson = json.encodeToString(finalTags),
                alternateGreetingsJson = json.encodeToString(updatedV3.data.alternateGreetings),
                rawJsonData = jsonStr,
                imagePath = destFile.absolutePath,
                isFavorite = false
            )

            cardDao.insertCard(entity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateCardTags(id: String, newTags: List<String>) = withContext(Dispatchers.IO) {
        val existingCard = cardDao.getCardById(id) ?: return@withContext
        val effectiveTags = if (newTags.isEmpty()) listOf("未分类") else newTags
        val tagsJson = json.encodeToString(effectiveTags)

        val updatedRawJson = try {
            val v3 = json.decodeFromString<CharacterCardV3>(existingCard.rawJsonData)
            val updatedV3 = v3.copy(data = v3.data.copy(tags = effectiveTags))
            val jsonStr = json.encodeToString(updatedV3)
            
            val file = File(existingCard.imagePath)
            if (file.exists()) {
                val updatedPngBytes = PngChunkUtils.injectJsonIntoPng(file.readBytes(), jsonStr)
                file.writeBytes(updatedPngBytes)
            }
            jsonStr
        } catch (e: Exception) {
            existingCard.rawJsonData
        }

        val updatedEntity = existingCard.copy(
            tagsJson = tagsJson,
            rawJsonData = updatedRawJson,
            updatedAt = System.currentTimeMillis()
        )
        cardDao.updateCard(updatedEntity)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        cardDao.setFavorite(id, isFavorite)
    }

    suspend fun deleteCard(card: CardEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(card.imagePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cardDao.deleteCard(card)
    }

    private fun createDefaultCardV3(name: String): CharacterCardV3 {
        return CharacterCardV3(
            data = CardData(
                name = name,
                description = "导入的角色卡详细信息...",
                tags = listOf("默认")
            )
        )
    }
}
