package com.charavault.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.charavault.app.data.local.AppDatabase
import com.charavault.app.data.local.CardEntity
import com.charavault.app.data.model.CharacterCardV3
import com.charavault.app.data.parser.CardValidator
import com.charavault.app.data.parser.PngChunkUtils
import com.charavault.app.data.parser.ValidationResult
import com.charavault.app.data.parser.ZipUnpacker
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
    val scannedCount: Int,
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
    suspend fun updateCardsOrder(category: String, cardIdsInOrder: List<String>) = withContext(Dispatchers.IO) {
        if (category.isBlank() || cardIdsInOrder.isEmpty()) return@withContext

        val updatedCards = cardIdsInOrder.mapIndexedNotNull { index, id ->
            val card = cardDao.getCardById(id) ?: return@mapIndexedNotNull null
            val currentOrders = try {
                json.decodeFromString<Map<String, Int>>(card.categorySortOrdersJson)
            } catch (_: Exception) {
                emptyMap()
            }
            card.copy(categorySortOrdersJson = json.encodeToString(withCategoryOrder(currentOrders, category, index)))
        }
        if (updatedCards.size == cardIdsInOrder.size) cardDao.updateCards(updatedCards)
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

        // Re-calculate semantic hash and core normalized JSON hash
        val semanticString = "${updatedV3.data.name.trim().lowercase()}|${updatedV3.data.creator.trim().lowercase()}|${updatedV3.data.description.trim()}"
        val semanticHash = CardValidator.calculateSha256(semanticString.toByteArray(Charsets.UTF_8))
        val normalizedJsonHash = CardValidator.calculateNormalizedCoreJsonHash(updatedV3)

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
            normalizedJsonHash = normalizedJsonHash,
            semanticHash = semanticHash,
            updatedAt = System.currentTimeMillis()
        )
        cardDao.updateCard(updatedEntity)
    }

    /**
     * Unified Batch Import Pipeline (PNG / JSON / ZIP)
     * Handles format auto-dispatch, ZIP unpacking, format validation, 3-layer hash deduplication, and transactional Room inserts
     */
    suspend fun importCardStreamsBatch(
        items: List<Pair<InputStream, String>>,
        selectedTags: List<String> = emptyList()
    ): BatchImportResult = withContext(Dispatchers.IO) {
        backfillMissingNormalizedJsonHashes()

        var successCount = 0
        var failedCount = 0
        var duplicateCount = 0
        var scannedCount = 0
        val failedReasons = mutableListOf<String>()

        // In-memory sets to track deduplication within the current import batch
        val batchFileHashes = mutableSetOf<String>()
        val batchNormalizedJsonHashes = mutableSetOf<String>()
        val batchSemanticHashes = mutableSetOf<String>()

        val createdFiles = mutableListOf<File>()

        try {
            items.forEach { (inputStream, fileName) ->
                try {
                    val bytes = inputStream.use { it.readBytes() }
                    if (ZipUnpacker.isZip(bytes, fileName)) {
                        try {
                            val entries = ZipUnpacker.unpackZipBytes(bytes)
                            if (entries.isEmpty()) {
                                failedCount++
                                failedReasons.add("$fileName: ZIP 压缩包内未包含有效 PNG 或 JSON 角色卡文件")
                            } else {
                                entries.forEach { entry ->
                                    scannedCount++
                                    val res = processCardCandidate(
                                        bytes = entry.bytes,
                                        fileName = entry.fileName,
                                        selectedTags = selectedTags,
                                        batchFileHashes = batchFileHashes,
                                        batchNormalizedJsonHashes = batchNormalizedJsonHashes,
                                        batchSemanticHashes = batchSemanticHashes,
                                        createdFiles = createdFiles
                                    )
                                    when (res) {
                                        is CandidateResult.Success -> successCount++
                                        is CandidateResult.Duplicate -> {
                                            duplicateCount++
                                            failedReasons.add(res.reason)
                                        }
                                        is CandidateResult.Invalid -> {
                                            failedCount++
                                            failedReasons.add(res.reason)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            failedCount++
                            failedReasons.add("$fileName: ZIP 解包失败 (${e.message ?: "解包异常"})")
                        }
                    } else {
                        scannedCount++
                        val res = processCardCandidate(
                            bytes = bytes,
                            fileName = fileName,
                            selectedTags = selectedTags,
                            batchFileHashes = batchFileHashes,
                            batchNormalizedJsonHashes = batchNormalizedJsonHashes,
                            batchSemanticHashes = batchSemanticHashes,
                            createdFiles = createdFiles
                        )
                        when (res) {
                            is CandidateResult.Success -> successCount++
                            is CandidateResult.Duplicate -> {
                                duplicateCount++
                                failedReasons.add(res.reason)
                            }
                            is CandidateResult.Invalid -> {
                                failedCount++
                                failedReasons.add(res.reason)
                            }
                        }
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedReasons.add("$fileName: 读取文件失败")
                }
            }
        } catch (e: Exception) {
            // Clean up any written files if critical pipeline failure occurs
            createdFiles.forEach { file -> if (file.exists()) file.delete() }
            throw e
        }

        BatchImportResult(successCount, failedCount, duplicateCount, scannedCount, failedReasons)
    }

    private sealed class CandidateResult {
        object Success : CandidateResult()
        data class Duplicate(val reason: String) : CandidateResult()
        data class Invalid(val reason: String) : CandidateResult()
    }

    private suspend fun processCardCandidate(
        bytes: ByteArray,
        fileName: String,
        selectedTags: List<String>,
        batchFileHashes: MutableSet<String>,
        batchNormalizedJsonHashes: MutableSet<String>,
        batchSemanticHashes: MutableSet<String>,
        createdFiles: MutableList<File>
    ): CandidateResult {
        return when (val validation = CardValidator.validateCardData(bytes, fileName)) {
            is ValidationResult.Success -> {
                val cardV3 = validation.cardV3
                val fileHash = validation.fileHash
                val normalizedJsonHash = validation.normalizedJsonHash
                val semanticHash = validation.semanticHash

                // 1. Three-Layer Hash Deduplication Check (fileHash -> normalizedJsonHash -> semanticHash)
                val isDuplicateFile = batchFileHashes.contains(fileHash) || cardDao.getCardByFileHash(fileHash) != null
                val isDuplicateJson = !isDuplicateFile && (batchNormalizedJsonHashes.contains(normalizedJsonHash) || cardDao.getCardByNormalizedJsonHash(normalizedJsonHash) != null)
                val isDuplicateSemantic = !isDuplicateFile && !isDuplicateJson && (batchSemanticHashes.contains(semanticHash) || cardDao.getCardBySemanticHash(semanticHash) != null)

                if (isDuplicateFile || isDuplicateJson || isDuplicateSemantic) {
                    val reasonType = when {
                        isDuplicateFile -> "完全重复"
                        isDuplicateJson -> "JSON 内容重复"
                        else -> "同角色重复"
                    }
                    val nameStr = cardV3.data.name.ifBlank { fileName }
                    return CandidateResult.Duplicate("重复卡片: [$nameStr] ($reasonType)")
                }

                // 2. Insert New Card with Room Database Transaction
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

                val finalPngBytes = PngChunkUtils.injectJsonIntoPng(validation.rawPngBytes, jsonStr)
                destFile.writeBytes(finalPngBytes)
                createdFiles.add(destFile)

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
                    normalizedJsonHash = normalizedJsonHash,
                    semanticHash = semanticHash
                )

                db.withTransaction {
                    cardDao.insertCard(entity)
                }

                // Track in current batch
                batchFileHashes.add(fileHash)
                batchNormalizedJsonHashes.add(normalizedJsonHash)
                batchSemanticHashes.add(semanticHash)

                CandidateResult.Success
            }
            is ValidationResult.Invalid -> {
                CandidateResult.Invalid("$fileName: ${validation.reason}")
            }
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

        val parsedV3 = try { json.decodeFromString<CharacterCardV3>(updatedRawJson) } catch (_: Exception) { null }
        val normalizedJsonHash = if (parsedV3 != null) {
            CardValidator.calculateNormalizedCoreJsonHash(parsedV3)
        } else {
            existingCard.normalizedJsonHash
        }

        val updatedEntity = existingCard.copy(
            tagsJson = tagsJson,
            rawJsonData = updatedRawJson,
            normalizedJsonHash = normalizedJsonHash,
            updatedAt = System.currentTimeMillis()
        )
        cardDao.updateCard(updatedEntity)
    }

    suspend fun updateCardAvatar(context: Context, id: String, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val existingCard = cardDao.getCardById(id) ?: return@withContext false
        var tempFile: File? = null
        try {
            val newImageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: return@withContext false

            if (newImageBytes.isEmpty()) return@withContext false

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(newImageBytes, 0, newImageBytes.size)
                ?: return@withContext false
            val rawPng = try {
                val baos = java.io.ByteArrayOutputStream()
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)) {
                    return@withContext false
                }
                baos.toByteArray()
            } finally {
                bitmap.recycle()
            }
            val finalPngBytes = PngChunkUtils.injectJsonIntoPng(rawPng, existingCard.rawJsonData)

            val destFile = File(existingCard.imagePath)
            tempFile = File(destFile.parentFile, ".${destFile.name}.tmp")
            tempFile.writeBytes(finalPngBytes)
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            val newFileHash = CardValidator.calculateSha256(finalPngBytes)

            val updatedEntity = existingCard.copy(
                fileHash = newFileHash,
                updatedAt = System.currentTimeMillis()
            )
            cardDao.updateCard(updatedEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            tempFile?.takeIf { it.exists() }?.delete()
        }
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

    private suspend fun backfillMissingNormalizedJsonHashes() {
        val cards = cardDao.getCardsMissingNormalizedJsonHash()
        if (cards.isEmpty()) return

        val updatedCards = cards.mapNotNull { card ->
            val parsed = PngChunkUtils.parseRawJsonToV3(card.rawJsonData) ?: return@mapNotNull null
            val normalizedJsonHash = CardValidator.calculateNormalizedCoreJsonHash(parsed)
            card.copy(normalizedJsonHash = normalizedJsonHash)
        }
        if (updatedCards.isNotEmpty()) {
            cardDao.updateCards(updatedCards)
        }
    }
}

internal fun withCategoryOrder(
    currentOrders: Map<String, Int>,
    category: String,
    order: Int
): Map<String, Int> = currentOrders + (category to order)
