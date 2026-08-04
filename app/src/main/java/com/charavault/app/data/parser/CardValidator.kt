package com.charavault.app.data.parser

import com.charavault.app.data.model.CharacterCardV3
import java.io.ByteArrayInputStream

sealed class ValidationResult {
    data class Success(val cardV3: CharacterCardV3) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

object CardValidator {

    private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB Limit

    /**
     * Perform compliance validation on imported PNG character card file
     */
    fun validatePngCard(bytes: ByteArray, fileName: String): ValidationResult {
        // 1. Check file size
        if (bytes.isEmpty()) {
            return ValidationResult.Invalid("文件为空")
        }
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.Invalid("文件过大（超过50MB）")
        }

        // 2. Check PNG Header
        if (!isPngHeader(bytes)) {
            return ValidationResult.Invalid("非合法 PNG 图片格式")
        }

        // 3. Extract and validate Character Card JSON V3/V2
        val cardV3 = PngChunkUtils.extractCardFromJsonPng(ByteArrayInputStream(bytes))
            ?: return ValidationResult.Invalid("未检测到标准角色卡 (chara/ccv3) 节点")

        val name = cardV3.data.name.ifBlank { fileName.substringBeforeLast(".") }
        if (name.isBlank()) {
            return ValidationResult.Invalid("角色卡缺少有效名称")
        }

        return ValidationResult.Success(cardV3.copy(data = cardV3.data.copy(name = name)))
    }

    private fun isPngHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        for (i in 0..7) {
            if (bytes[i] != pngHeader[i]) return false
        }
        return true
    }
}
