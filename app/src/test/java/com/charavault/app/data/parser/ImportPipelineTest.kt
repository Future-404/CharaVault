package com.charavault.app.data.parser

import com.charavault.app.data.model.CardData
import com.charavault.app.data.model.CharacterCardV3
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImportPipelineTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testZipUnpackerFlatExtractionAndFiltering() {
        val zipBytes = createMockZipBytes(
            entries = listOf(
                "chara1.png" to "fake-png-content".toByteArray(),
                "chara2.json" to """{"spec":"chara_card_v3","spec_version":"3.0","data":{"name":"Hero"}}""".toByteArray(),
                "subfolder/chara3.png" to "fake-png-content-2".toByteArray(),
                "nested.zip" to "zip-content".toByteArray(),
                "readme.txt" to "text content".toByteArray()
            )
        )

        assertTrue(ZipUnpacker.isZip(zipBytes, "test.zip"))

        val extracted = ZipUnpacker.unpackZipBytes(zipBytes)

        // Should extract 3 items (including the subfolder entry), skipping nested zip & readme.txt
        assertEquals(3, extracted.size)
        val names = extracted.map { it.fileName }
        assertTrue(names.contains("chara1.png"))
        assertTrue(names.contains("chara2.json"))
        assertTrue(names.contains("chara3.png"))
        assertFalse(names.contains("nested.zip"))
        assertFalse(names.contains("readme.txt"))
    }

    @Test
    fun testZipUnpackerKeepsNestedPathAsFileNameOnly() {
        val zipBytes = createMockZipBytes(
            entries = listOf(
                "nested/inner/chara4.png" to "fake-png-content-4".toByteArray()
            )
        )

        val extracted = ZipUnpacker.unpackZipBytes(zipBytes)

        assertEquals(1, extracted.size)
        assertEquals("chara4.png", extracted.first().fileName)
    }

    @Test
    fun testJsonCardValidationAndHashGeneration() {
        val validJson = """
            {
                "spec": "chara_card_v3",
                "spec_version": "3.0",
                "data": {
                    "name": "Kaguya",
                    "creator": "SillyTavern",
                    "description": "Student Council Vice President"
                }
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val result = CardValidator.validateCardData(validJson, "kaguya.json")

        assertTrue(result is ValidationResult.Success)
        val success = result as ValidationResult.Success
        assertEquals("Kaguya", success.cardV3.data.name)
        assertEquals("SillyTavern", success.cardV3.data.creator)
        assertTrue(success.fileHash.isNotBlank())
        assertTrue(success.normalizedJsonHash.isNotBlank())
        assertTrue(success.semanticHash.isNotBlank())
    }

    @Test
    fun testSemanticHashConsistency() {
        val card1 = CharacterCardV3(data = CardData(name = "   ALICE ", creator = "Bob  ", description = "   A brave girl.  "))
        val card2 = CharacterCardV3(data = CardData(name = "alice", creator = "bob", description = "A brave girl."))

        val semanticStr1 = "${card1.data.name.trim().lowercase()}|${card1.data.creator.trim().lowercase()}|${card1.data.description.trim()}"
        val semanticStr2 = "${card2.data.name.trim().lowercase()}|${card2.data.creator.trim().lowercase()}|${card2.data.description.trim()}"

        val hash1 = CardValidator.calculateSha256(semanticStr1.toByteArray(Charsets.UTF_8))
        val hash2 = CardValidator.calculateSha256(semanticStr2.toByteArray(Charsets.UTF_8))

        assertEquals(hash1, hash2)
    }

    @Test
    fun testNormalizedJsonHashConsistency() {
        val jsonStr1 = """{"spec":"chara_card_v3","spec_version":"3.0","data":{"name":"Tester","creator":"Dev"}}"""
        val parsed1 = PngChunkUtils.parseRawJsonToV3(jsonStr1)!!

        val norm1 = Json { ignoreUnknownKeys = true; prettyPrint = true }.encodeToString(parsed1)
        val hash1 = CardValidator.calculateSha256(norm1.toByteArray(Charsets.UTF_8))

        val parsed2 = PngChunkUtils.parseRawJsonToV3(norm1)!!
        val norm2 = Json { ignoreUnknownKeys = true; prettyPrint = true }.encodeToString(parsed2)
        val hash2 = CardValidator.calculateSha256(norm2.toByteArray(Charsets.UTF_8))

        assertEquals(hash1, hash2)
    }

    @Test
    fun testInvalidCardContentValidation() {
        val invalidResult = CardValidator.validateCardData("Not a valid json or png".toByteArray(), "bad.txt")
        assertTrue(invalidResult is ValidationResult.Invalid)

        val emptyResult = CardValidator.validateCardData(ByteArray(0), "empty.png")
        assertTrue(emptyResult is ValidationResult.Invalid)
    }

    @Test
    fun testJsonHashIgnoresWhitespaceFormatting() {
        val jsonA = """{"spec":"chara_card_v3","spec_version":"3.0","data":{"name":"Tester","creator":"Dev"}}"""
        val jsonB = """
            {
              "spec": "chara_card_v3",
              "spec_version": "3.0",
              "data": {
                "creator": "Dev",
                "name": "Tester"
              }
            }
        """.trimIndent()

        val parsedA = PngChunkUtils.parseRawJsonToV3(jsonA)!!
        val parsedB = PngChunkUtils.parseRawJsonToV3(jsonB)!!
        val normA = json.encodeToString(parsedA)
        val normB = json.encodeToString(parsedB)

        assertEquals(
            CardValidator.calculateSha256(normA.toByteArray(Charsets.UTF_8)),
            CardValidator.calculateSha256(normB.toByteArray(Charsets.UTF_8))
        )
    }

    private fun createMockZipBytes(entries: List<Pair<String, ByteArray>>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, bytes) ->
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
