package com.charavault.app.data.repository

import android.content.Context
import com.charavault.app.data.local.AppDatabase
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.data.parser.CardValidator
import com.charavault.app.data.parser.PngChunkUtils
import com.charavault.app.data.parser.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.UUID

data class BatchImportResult(
    val successCount: Int,
    val failedCount: Int,
    val duplicateCount: Int,
    val failedReasons: List<String>
)

class CardRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val cardDao = db.cardDao()
    private val cardsDir = File(context.getExternalFilesDir(null), "cards").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }

    fun getAllCards(): Flow<List<CardEntity>> = cardDao.getAllCardsFlow()

    /**
     * Update card sort order persistence
     */
    suspend fun updateCardsOrder(cardIdsInOrder: List<String>) = withContext(Dispatchers.IO) {
        cardIdsInOrder.forEachIndexed { index, id ->
            cardDao.updateSortOrder(id, index)
        }
    }

    /**
     * Full edit & update character card data, writing back to physical PNG & Room DB
     */
    suspend fun updateFullCardData(id: String, updatedV3: CharacterCardV3) = withContext(Dispatchers.IO) {
        val existingCard = cardDao.getCardById(id) ?: return@withContext
        val jsonStr = json.encodeToString(updatedV3)

        // Write back updated JSON to physical PNG chunk
        val file = File(existingCard.imagePath)
        if (file.exists()) {
            val updatedPngBytes = PngChunkUtils.injectJsonIntoPng(file.readBytes(), jsonStr)
            file.writeBytes(updatedPngBytes)
        }

        // Re-calculate semantic hash
        val semanticString = "${updatedV3.data.name.trim().lowercase()}|${updatedV3.data.creator.trim().lowercase()}|${updatedV3.data.description.trim()}"
        val semanticHash = CardValidator.calculateSha256(semanticString.toByteArray(Charsets.UTF_8))

        val updatedEntity = existingCard.copy(
            name = updatedV3.data.name,
            creator = updatedV3.data.creator.ifBlank { "未知作者" },
            description = updatedV3.data.description,
            personality = updatedV3.data.personality,
            scenario = updatedV3.data.scenario,
            firstMes = updatedV3.data.firstMes,
            systemPrompt = updatedV3.data.systemPrompt,
            alternateGreetingsJson = json.encodeToString(updatedV3.data.alternateGreetings),
            rawJsonData = jsonStr,
            semanticHash = semanticHash,
            updatedAt = System.currentTimeMillis()
        )
        cardDao.updateCard(updatedEntity)
    }

    /**
     * Batch import multiple cards with compliance validation & dual-layer hash de-duplication
     */
    suspend fun importCardStreamsBatch(
        items: List<Pair<InputStream, String>>,
        selectedTags: List<String> = emptyList()
    ): BatchImportResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failedCount = 0
        var duplicateCount = 0
        val failedReasons = mutableListOf<String>()

        items.forEach { (inputStream, fileName) ->
            try {
                val bytes = inputStream.readBytes()
                when (val validation = CardValidator.validatePngCard(bytes, fileName)) {
                    is ValidationResult.Success -> {
                        val cardV3 = validation.cardV3
                        val fileHash = validation.fileHash
                        val semanticHash = validation.semanticHash

                        // 1. Dual-layer Hash Deduplication Check
                        val existingByFileHash = cardDao.getCardByFileHash(fileHash)
                        val existingBySemanticHash = cardDao.getCardBySemanticHash(semanticHash)

                        if (existingByFileHash != null || existingBySemanticHash != null) {
                            duplicateCount++
                            val existingName = existingByFileHash?.name ?: existingBySemanticHash?.name ?: cardV3.data.name
                            failedReasons.add("重复卡片: [$existingName]")
                            return@forEach
                        }

                        // 2. Insert New Card
                        val id = UUID.randomUUID().toString().take(8)
                        val destFile = File(cardsDir, "chara_$id.png")

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

                        val entity = CardEntity(
                            id = id,
                            name = updatedV3.data.name,
                            creator = updatedV3.data.creator.ifBlank { "未知作者" },
                            description = updatedV3.data.description,
                            personality = updatedV3.data.personality,
                            scenario = updatedV3.data.scenario,
                            firstMes = updatedV3.data.firstMes,
                            systemPrompt = updatedV3.data.systemPrompt,
                            tagsJson = json.encodeToString(finalTags),
                            alternateGreetingsJson = json.encodeToString(updatedV3.data.alternateGreetings),
                            rawJsonData = jsonStr,
                            imagePath = destFile.absolutePath,
                            fileHash = fileHash,
                            semanticHash = semanticHash,
                            isFavorite = false
                        )

                        cardDao.insertCard(entity)
                        successCount++
                    }
                    is ValidationResult.Invalid -> {
                        failedCount++
                        failedReasons.add("$fileName: ${validation.reason}")
                    }
                }
            } catch (e: Exception) {
                failedCount++
                failedReasons.add("$fileName: 读取文件失败")
            }
        }

        BatchImportResult(successCount, failedCount, duplicateCount, failedReasons)
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

    suspend fun deleteCard(card: CardEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(card.imagePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cardDao.deleteCard(card)
    }
}
