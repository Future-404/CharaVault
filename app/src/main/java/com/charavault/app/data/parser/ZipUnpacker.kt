package com.charavault.app.data.parser

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ZipEntryData(
    val fileName: String,
    val bytes: ByteArray
)

object ZipUnpacker {

    private const val MAX_TOTAL_UNCOMPRESSED_SIZE = 2L * 1024 * 1024 * 1024 // 2GB total

    /**
     * Check if a given byte array or filename indicates a ZIP archive
     */
    fun isZip(bytes: ByteArray, fileName: String = ""): Boolean {
        if (fileName.endsWith(".zip", ignoreCase = true)) return true
        if (bytes.size >= 4) {
            val isPkHeader = bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())
            if (isPkHeader) return true
        }
        return false
    }

    /**
     * Unpack ZIP stream safely according to import pipeline rules:
     * - Extract matching files from root and subdirectories
     * - Ignore directories and nested zip archives (.zip)
     * - Extract only .png and .json files
     * - Enforce total uncompressed size bound against zip bomb attacks
     */
    fun unpackZip(inputStream: InputStream): List<ZipEntryData> {
        val results = mutableListOf<ZipEntryData>()
        ZipInputStream(inputStream).use { zipStream ->
            var totalExtractedSize = 0L
            val buffer = ByteArray(8192)

            while (true) {
                val entry = zipStream.nextEntry ?: break

                if (entry.isDirectory) {
                    zipStream.closeEntry()
                    continue
                }

                val simpleName = entry.name.substringAfterLast('/').substringAfterLast('\\').trim()
                if (simpleName.isBlank() || simpleName.startsWith(".")) {
                    zipStream.closeEntry()
                    continue
                }

                // Ignore nested zip files
                if (simpleName.endsWith(".zip", ignoreCase = true)) {
                    zipStream.closeEntry()
                    continue
                }

                // Only extract .png and .json entries
                val isPng = simpleName.endsWith(".png", ignoreCase = true)
                val isJson = simpleName.endsWith(".json", ignoreCase = true)

                if (!isPng && !isJson) {
                    zipStream.closeEntry()
                    continue
                }

                val baos = ByteArrayOutputStream()

                while (true) {
                    val bytesRead = zipStream.read(buffer)
                    if (bytesRead <= 0) break

                    totalExtractedSize += bytesRead
                    if (totalExtractedSize > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                        zipStream.closeEntry()
                        throw IllegalArgumentException("ZIP 解包总尺寸超过限制 (2GB)")
                    }

                    baos.write(buffer, 0, bytesRead)
                }

                results.add(ZipEntryData(simpleName, baos.toByteArray()))
                zipStream.closeEntry()
            }
        }

        return results
    }

    fun unpackZipBytes(bytes: ByteArray): List<ZipEntryData> {
        return unpackZip(ByteArrayInputStream(bytes))
    }
}
