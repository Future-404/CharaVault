package com.charavault.app.data.parser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.charavault.app.data.model.CharacterCardV3
import java.io.ByteArrayOutputStream

object DefaultAvatarGenerator {

    private val FALLBACK_PNG_BYTES = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F.toByte(), 0x15, 0xC4.toByte(), 0x89.toByte(),
        0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
        0x78, 0x9C.toByte(), 0x63, 0x60, 0x00, 0x00, 0x00, 0x02,
        0x00, 0x01, 0xE5.toByte(), 0x27.toByte(), 0xD4.toByte(), 0xA7.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
    )

    /**
     * Generate a 512x512 default card avatar PNG with character initial & embedded V3 JSON chunk
     */
    fun generateDefaultPngWithJson(cardV3: CharacterCardV3, jsonStr: String): ByteArray {
        return try {
            generateWithBitmap(cardV3, jsonStr)
        } catch (_: RuntimeException) {
            PngChunkUtils.injectJsonIntoPng(FALLBACK_PNG_BYTES, jsonStr)
        } catch (_: LinkageError) {
            PngChunkUtils.injectJsonIntoPng(FALLBACK_PNG_BYTES, jsonStr)
        }
    }

    private fun generateWithBitmap(cardV3: CharacterCardV3, jsonStr: String): ByteArray {
        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Linear Gradient Background
        val bgGradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#4C1D95"), Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = bgGradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Subtle Inner Frame Card
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val frameRect = RectF(24f, 24f, width - 24f, height - 24f)
        canvas.drawRoundRect(frameRect, 32f, 32f, framePaint)

        // 3. Draw Center Circle Badge
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#338B5CF6")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, height / 2f - 20f, 120f, circlePaint)

        val circleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#808B5CF6")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(width / 2f, height / 2f - 20f, 120f, circleBorder)

        // 4. Draw Initial Character Letter in Center
        val charName = cardV3.data.name.ifBlank { "Chara" }
        val initialChar = charName.trim().firstOrNull()?.toString()?.uppercase() ?: "C"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 120f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val textBaseline = (height / 2f - 20f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(initialChar, width / 2f, textBaseline, textPaint)

        // 5. Draw Card Name & V3 Watermark at Bottom
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val truncatedName = if (charName.length > 16) charName.take(15) + "…" else charName
        canvas.drawText(truncatedName, width / 2f, height - 70f, namePaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#99A78BFA")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CharaVault · V3 Spec Card", width / 2f, height - 42f, badgePaint)

        // 6. Compress Bitmap to PNG Byte Array
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val rawPngBytes = outputStream.toByteArray()

        // 7. Inject V3 JSON String into PNG tEXt Chunk ('chara' / 'ccv3')
        return PngChunkUtils.injectJsonIntoPng(rawPngBytes, jsonStr)
    }
}
