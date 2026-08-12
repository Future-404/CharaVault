package com.charavault.app.data.parser

import com.charavault.app.data.model.CharacterCardV3
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.security.MessageDigest

sealed class ValidationResult {
    data class Success(
        val cardV3: CharacterCardV3,
        val rawPngBytes: ByteArray,
        val fileHash: String,
        val normalizedJsonHash: String,
        val semanticHash: String
    ) : ValidationResult()

    data class Invalid(val reason: String) : ValidationResult()
}

object CardValidator {

    private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB Limit
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    /**
     * Calculate normalized Core JSON Hash, stripping variable runtime attributes (such as tags)
     * to ensure deduplication remains reliable even after users modify category tags in App.
     */
    fun calculateNormalizedCoreJsonHash(cardV3: CharacterCardV3): String {
        val coreV3 = cardV3.copy(data = cardV3.data.copy(tags = emptyList()))
        val coreJsonStr = json.encodeToString(coreV3)
        return calculateSha256(coreJsonStr.toByteArray(Charsets.UTF_8))
    }

    /**
     * Perform compliance validation & 3-layer hash calculation on imported PNG or JSON character card
     */
    fun validateCardData(bytes: ByteArray, fileName: String): ValidationResult {
        // 1. Check file size
        if (bytes.isEmpty()) {
            return ValidationResult.Invalid("文件内容为空")
        }
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.Invalid("文件尺寸过大 (超过50MB)")
        }

        // 2. Option A: Check PNG Magic Header
        if (isPngHeader(bytes)) {
            val cardV3 = PngChunkUtils.extractCardFromJsonPng(ByteArrayInputStream(bytes))
                ?: return ValidationResult.Invalid("PNG 图片未包含标准角色卡数据 (chara/ccv3)")

            val name = cardV3.data.name.ifBlank { fileName.substringBeforeLast(".") }
            if (name.isBlank()) {
                return ValidationResult.Invalid("角色卡缺少有效名称")
            }

            val normalizedV3 = cardV3.copy(data = cardV3.data.copy(name = name))

            val fileHash = calculateSha256(bytes)
            val normalizedJsonHash = calculateNormalizedCoreJsonHash(normalizedV3)
            val semanticString = "${normalizedV3.data.name.trim().lowercase()}|${normalizedV3.data.creator.trim().lowercase()}|${normalizedV3.data.description.trim()}"
            val semanticHash = calculateSha256(semanticString.toByteArray(Charsets.UTF_8))

            return ValidationResult.Success(
                cardV3 = normalizedV3,
                rawPngBytes = bytes,
                fileHash = fileHash,
                normalizedJsonHash = normalizedJsonHash,
                semanticHash = semanticHash
            )
        }

        // 3. Option B: Try parsing as Standalone JSON Character Card
        val jsonStr = try {
            String(bytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            return ValidationResult.Invalid("非标准 UTF-8 编码文本")
        }

        if (!jsonStr.startsWith("{") || !jsonStr.endsWith("}")) {
            return ValidationResult.Invalid("非合法 PNG 图片或 JSON 角色卡格式")
        }

        val cardV3 = PngChunkUtils.parseRawJsonToV3(jsonStr)
            ?: return ValidationResult.Invalid("JSON 未包含标准角色卡规范 (缺少 spec/data 或角色名称/人设字段)")

        val name = cardV3.data.name.ifBlank { fileName.substringBeforeLast(".") }
        if (name.isBlank()) {
            return ValidationResult.Invalid("角色卡缺少有效名称")
        }

        val normalizedV3 = cardV3.copy(data = cardV3.data.copy(name = name))
        val normalizedJsonStr = json.encodeToString(normalizedV3)

        // Generate Default PNG Card Avatar with embedded V3 JSON chunk
        val defaultAvatarPngBytes = DefaultAvatarGenerator.generateDefaultPngWithJson(normalizedV3, normalizedJsonStr)

        val fileHash = calculateSha256(defaultAvatarPngBytes)
        val normalizedJsonHash = calculateNormalizedCoreJsonHash(normalizedV3)
        val semanticString = "${normalizedV3.data.name.trim().lowercase()}|${normalizedV3.data.creator.trim().lowercase()}|${normalizedV3.data.description.trim()}"
        val semanticHash = calculateSha256(semanticString.toByteArray(Charsets.UTF_8))

        return ValidationResult.Success(
            cardV3 = normalizedV3,
            rawPngBytes = defaultAvatarPngBytes,
            fileHash = fileHash,
            normalizedJsonHash = normalizedJsonHash,
            semanticHash = semanticHash
        )
    }

    /**
     * Backward-compatible alias for validateCardData
     */
    fun validatePngCard(bytes: ByteArray, fileName: String): ValidationResult = validateCardData(bytes, fileName)

    private fun isPngHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        for (i in 0..7) {
            if (bytes[i] != pngHeader[i]) return false
        }
        return true
    }

    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
