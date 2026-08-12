package com.charavault.app.data.parser

import android.annotation.SuppressLint
import com.charavault.app.data.model.CharacterCardV3
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

object PngChunkUtils {

    private val PNG_HEADER = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Extract CharacterCardV3 JSON from PNG Input Stream
     */
    fun extractCardFromJsonPng(inputStream: InputStream): CharacterCardV3? {
        val bytes = inputStream.readBytes()
        if (!isPng(bytes)) return null

        val jsonStr = extractRawJsonFromPngBytes(bytes) ?: return null
        return parseRawJsonToV3(jsonStr)
    }

    /**
     * Parse raw JSON string (V3 spec or V2 CardData) into CharacterCardV3
     */
    fun parseRawJsonToV3(jsonStr: String): CharacterCardV3? {
        return try {
            val v3 = jsonParser.decodeFromString<CharacterCardV3>(jsonStr)
            if (v3.spec == "chara_card_v3" || v3.data.name.isNotBlank() || v3.data.description.isNotBlank() || v3.data.firstMes.isNotBlank()) {
                v3
            } else {
                null
            }
        } catch (e: Exception) {
            try {
                val data = jsonParser.decodeFromString<com.charavault.app.data.model.CardData>(jsonStr)
                if (data.name.isNotBlank() || data.description.isNotBlank() || data.personality.isNotBlank() || data.firstMes.isNotBlank() || data.scenario.isNotBlank()) {
                    CharacterCardV3(data = data)
                } else {
                    null
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Extract raw JSON string from PNG bytes (looking for 'chara' tEXt/iTXt chunk)
     */
    fun extractRawJsonFromPngBytes(bytes: ByteArray): String? {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(8) // Skip PNG Header

        while (buffer.remaining() >= 12) {
            val length = buffer.int
            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, StandardCharsets.US_ASCII)

            if (length < 0 || buffer.remaining() < length + 4) break

            val dataBytes = ByteArray(length)
            buffer.get(dataBytes)
            buffer.int // skip CRC

            if (type == "tEXt" || type == "iTXt") {
                val parsed = parseTextChunk(dataBytes)
                if (parsed != null && (parsed.first == "chara" || parsed.first == "ccv3")) {
                    return decodeValue(parsed.second)
                }
            } else if (type == "IEND") {
                break
            }
        }
        return null
    }

    /**
     * Inject JSON V3 back into PNG bytes (returns new PNG bytes with updated 'chara' tEXt chunk)
     */
    fun injectJsonIntoPng(originalPngBytes: ByteArray, jsonString: String): ByteArray {
        if (!isPng(originalPngBytes)) return originalPngBytes

        val encodedValue = encodeBase64(jsonString.toByteArray(StandardCharsets.UTF_8))
        val key = "chara"
        val textDataStream = ByteArrayOutputStream()
        textDataStream.write(key.toByteArray(StandardCharsets.US_ASCII))
        textDataStream.write(0) // Null separator
        textDataStream.write(encodedValue.toByteArray(StandardCharsets.US_ASCII))
        val chunkData = textDataStream.toByteArray()

        val output = ByteArrayOutputStream()
        output.write(PNG_HEADER)

        val buffer = ByteBuffer.wrap(originalPngBytes)
        buffer.position(8)

        var inserted = false

        while (buffer.remaining() >= 12) {
            val length = buffer.int
            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, StandardCharsets.US_ASCII)

            val dataBytes = ByteArray(length)
            buffer.get(dataBytes)
            buffer.int // skip CRC

            if (type == "tEXt") {
                val parsed = parseTextChunk(dataBytes)
                if (parsed?.first == "chara") {
                    // Skip existing 'chara' chunk to replace it
                    continue
                }
            }

            if (type == "IEND" && !inserted) {
                // Insert our new 'chara' tEXt chunk right before IEND
                writeChunk(output, "tEXt", chunkData)
                inserted = true
            }

            writeChunk(output, type, dataBytes)
        }

        return output.toByteArray()
    }

    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        for (i in 0..7) {
            if (bytes[i] != PNG_HEADER[i]) return false
        }
        return true
    }

    private fun parseTextChunk(chunkData: ByteArray): Pair<String, String>? {
        var nullIndex = -1
        for (i in chunkData.indices) {
            if (chunkData[i] == 0.toByte()) {
                nullIndex = i
                break
            }
        }
        if (nullIndex == -1) return null
        val key = String(chunkData, 0, nullIndex, StandardCharsets.US_ASCII)
        val value = String(chunkData, nullIndex + 1, chunkData.size - (nullIndex + 1), StandardCharsets.UTF_8)
        return Pair(key, value)
    }

    @SuppressLint("NewApi")
    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    @SuppressLint("NewApi")
    private fun decodeBase64(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str.trim(), android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str.trim())
        }
    }

    private fun decodeValue(rawValue: String): String {
        return try {
            val decoded = decodeBase64(rawValue)
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // If it's not base64 encoded, return as raw UTF-8 string
            rawValue
        }
    }

    private fun writeChunk(out: OutputStream, type: String, data: ByteArray) {
        val length = data.size
        val lengthBuf = ByteBuffer.allocate(4).putInt(length).array()
        val typeBuf = type.toByteArray(StandardCharsets.US_ASCII)

        out.write(lengthBuf)
        out.write(typeBuf)
        out.write(data)

        val crc32 = CRC32()
        crc32.update(typeBuf)
        crc32.update(data)
        val crcVal = crc32.value.toInt()
        val crcBuf = ByteBuffer.allocate(4).putInt(crcVal).array()
        out.write(crcBuf)
    }
}
