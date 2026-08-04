package com.charavault.app.data.parser

import com.charavault.app.data.model.CharacterCardV3
import java.io.ByteArrayInputStream
import java.security.MessageDigest

sealed class ValidationResult {
    data class Success(
        val cardV3: CharacterCardV3,
        val fileHash: String,
        val semanticHash: String
    ) : ValidationResult()

    data class Invalid(val reason: String) : ValidationResult()
}

object CardValidator {

    private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB Limit

    /**
     * Perform compliance validation & hash calculation on imported PNG character card
     */
    fun validatePngCard(bytes: ByteArray, fileName: String): ValidationResult {
        // 1. Check file size
        if (bytes.isEmpty()) {
            return ValidationResult.Invalid("文件内容为空")
        }
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.Invalid("文件尺寸过大 (超过50MB)")
        }

        // 2. Check PNG Magic Header
        if (!isPngHeader(bytes)) {
            return ValidationResult.Invalid("非合法 PNG 图片格式")
        }

        // 3. Extract and validate Character Card JSON V3/V2
        val cardV3 = PngChunkUtils.extractCardFromJsonPng(ByteArrayInputStream(bytes))
            ?: return ValidationResult.Invalid("未包含标准角色卡数据 (chara/ccv3)")

        val name = cardV3.data.name.ifBlank { fileName.substringBeforeLast(".") }
        if (name.isBlank()) {
            return ValidationResult.Invalid("角色卡缺少有效名称")
        }

        val normalizedV3 = cardV3.copy(data = cardV3.data.copy(name = name))

        // 4. Calculate Hashes
        val fileHash = calculateSha256(bytes)
        val semanticString = "${normalizedV3.data.name.trim().lowercase()}|${normalizedV3.data.creator.trim().lowercase()}|${normalizedV3.data.description.trim()}"
        val semanticHash = calculateSha256(semanticString.toByteArray(Charsets.UTF_8))

        return ValidationResult.Success(
            cardV3 = normalizedV3,
            fileHash = fileHash,
            semanticHash = semanticHash
        )
    }

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
