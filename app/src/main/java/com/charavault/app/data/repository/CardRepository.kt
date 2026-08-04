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
import java.io.File
import java.io.InputStream
import java.util.UUID

class CardRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val cardDao = db.cardDao()
    private val cardsDir = File(context.getExternalFilesDir(null), "cards").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun getAllCards(): Flow<List<CardEntity>> = cardDao.getAllCardsFlow()

    suspend fun importCardFromStream(inputStream: InputStream, originalFileName: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            val cardV3 = PngChunkUtils.extractCardFromJsonPng(ByteArrayInputStream(bytes))
                ?: createDefaultCardV3(originalFileName ?: "New Character")

            val id = UUID.randomUUID().toString().take(8)
            val destFile = File(cardsDir, "chara_$id.png")

            // Ensure PNG has embedded JSON V3
            val jsonStr = json.encodeToString(cardV3)
            val finalPngBytes = PngChunkUtils.injectJsonIntoPng(bytes, jsonStr)
            destFile.writeBytes(finalPngBytes)

            val tags = if (cardV3.data.tags.isEmpty()) listOf("未分类") else cardV3.data.tags

            val entity = CardEntity(
                id = id,
                name = cardV3.data.name.ifBlank { originalFileName?.substringBeforeLast(".") ?: "未命名角色" },
                creator = cardV3.data.creator.ifBlank { "未知作者" },
                description = cardV3.data.description,
                personality = cardV3.data.personality,
                scenario = cardV3.data.scenario,
                firstMes = cardV3.data.firstMes,
                systemPrompt = cardV3.data.systemPrompt,
                tagsJson = json.encodeToString(tags),
                alternateGreetingsJson = json.encodeToString(cardV3.data.alternateGreetings),
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
