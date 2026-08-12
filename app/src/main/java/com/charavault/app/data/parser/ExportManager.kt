package com.charavault.app.data.parser

import android.content.Context
import android.net.Uri
import com.charavault.app.data.local.CardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat {
    PNG, JSON
}

object ExportManager {

    /**
     * Generate default zip backup filename with timestamp
     */
    fun generateBackupZipFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "CharaVault_Backup_${dateFormat.format(Date())}.zip"
    }

    /**
     * Export a single character card PNG to target Uri
     */
    suspend fun exportSingleCardToUri(context: Context, card: CardEntity, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(card.imagePath)
            if (!sourceFile.exists()) return@withContext false

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export a single character card raw V3 JSON spec file (.json) to target Uri
     */
    suspend fun exportSingleCardJsonToUri(context: Context, card: CardEntity, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = card.rawJsonData
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                output.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export all character cards into a single Zip archive stream
     */
    suspend fun exportAllCardsToZip(context: Context, cards: List<CardEntity>, targetZipUri: Uri): Int = withContext(Dispatchers.IO) {
        var exportedCount = 0
        try {
            context.contentResolver.openOutputStream(targetZipUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    cards.forEach { card ->
                        val imageFile = File(card.imagePath)
                        if (imageFile.exists()) {
                            val sanitizeName = card.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                            val zipEntryName = "${sanitizeName}_${card.id}.png"

                            zipOut.putNextEntry(ZipEntry(zipEntryName))
                            FileInputStream(imageFile).use { fileIn ->
                                fileIn.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                            exportedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        exportedCount
    }
}
