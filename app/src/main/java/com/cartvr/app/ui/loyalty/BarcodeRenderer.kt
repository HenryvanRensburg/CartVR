package com.cartvr.app.ui.loyalty

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

sealed interface BarcodeRenderResult {
    data class Rendered(
        val bitmap: Bitmap,
        val usedFormat: String,
        val usedFallbackFormat: Boolean
    ) : BarcodeRenderResult

    data class FallbackTextOnly(
        val displayNumber: String,
        val reason: String
    ) : BarcodeRenderResult
}

object BarcodeUtils {

    val supportedFormats: List<Pair<String, String>> = listOf(
        "CODE_128" to "Standard (Code 128)",
        "EAN_13" to "EAN-13 (13 digits)",
        "EAN_8" to "EAN-8 (8 digits)",
        "UPC_A" to "UPC-A (12 digits)",
        "CODE_39" to "Code 39",
        "ITF" to "ITF",
        "QR_CODE" to "QR Code"
    )

    fun mlkitFormatToName(format: Int): String = when (format) {
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_PDF417 -> "PDF_417"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        else -> "CODE_128"
    }

    fun formatLabel(formatName: String): String = when (formatName.uppercase()) {
        "EAN_13" -> "EAN-13"
        "EAN_8" -> "EAN-8"
        "UPC_A" -> "UPC-A"
        "UPC_E" -> "UPC-E"
        "CODE_128" -> "Code 128"
        "CODE_39" -> "Code 39"
        "CODE_93" -> "Code 93"
        "ITF" -> "ITF"
        "CODABAR" -> "Codabar"
        "QR_CODE" -> "QR Code"
        "AZTEC" -> "Aztec"
        "PDF_417" -> "PDF-417"
        "DATA_MATRIX" -> "Data Matrix"
        else -> "Code 128"
    }

    fun normalizeNumberForFormat(raw: String, formatName: String): String {
        val trimmed = raw.trim()
        return if (formatName.equals("QR_CODE", ignoreCase = true) ||
            formatName.equals("AZTEC", ignoreCase = true) ||
            formatName.equals("PDF_417", ignoreCase = true) ||
            formatName.equals("DATA_MATRIX", ignoreCase = true)
        ) {
            trimmed
        } else {
            trimmed.replace(Regex("[\\s-]+"), "")
        }
    }

    fun formatSpacedDisplayNumber(raw: String): String {
        val trimmed = raw.trim()
        val compact = trimmed.replace(Regex("[\\s-]+"), "")
        return if (compact.length >= 8 && compact.all { it.isDigit() }) {
            compact.chunked(4).joinToString(" ")
        } else {
            trimmed
        }
    }

    fun is2DFormat(formatName: String): Boolean = formatName.uppercase() in setOf(
        "QR_CODE", "AZTEC", "DATA_MATRIX"
    )

    private fun toZxingFormat(formatName: String): BarcodeFormat = when (formatName.uppercase()) {
        "EAN_13" -> BarcodeFormat.EAN_13
        "EAN_8" -> BarcodeFormat.EAN_8
        "UPC_A" -> BarcodeFormat.UPC_A
        "UPC_E" -> BarcodeFormat.UPC_E
        "CODE_128" -> BarcodeFormat.CODE_128
        "CODE_39" -> BarcodeFormat.CODE_39
        "CODE_93" -> BarcodeFormat.CODE_93
        "ITF" -> BarcodeFormat.ITF
        "CODABAR" -> BarcodeFormat.CODABAR
        "QR_CODE" -> BarcodeFormat.QR_CODE
        "AZTEC" -> BarcodeFormat.AZTEC
        "PDF_417" -> BarcodeFormat.PDF_417
        "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
        else -> BarcodeFormat.CODE_128
    }

    /**
     * Safely encodes [rawValue] into a high-contrast black-on-white [Bitmap].
     * Automatically strips spaces/dashes for 1D barcodes and falls back to CODE_128 or text-only
     * so invalid check digits or formats never crash the till display dialog.
     */
    fun encodeBarcodeBitmap(
        rawValue: String,
        formatName: String,
        widthPx: Int = 960,
        heightPx: Int = 320
    ): BarcodeRenderResult {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return BarcodeRenderResult.FallbackTextOnly("", "No card number saved")
        }

        val requestedUpper = formatName.uppercase().ifBlank { "CODE_128" }
        val normalized = normalizeNumberForFormat(trimmed, requestedUpper)
        if (normalized.isEmpty()) {
            return BarcodeRenderResult.FallbackTextOnly(trimmed, "No valid characters to encode")
        }

        val primaryAttempt = tryEncode(normalized, requestedUpper, widthPx, heightPx)
        if (primaryAttempt != null) {
            return BarcodeRenderResult.Rendered(
                bitmap = primaryAttempt,
                usedFormat = requestedUpper,
                usedFallbackFormat = false
            )
        }

        // Safe fallback to CODE_128 when specific 1D check digit / length validation fails
        val compact = trimmed.replace(Regex("[\\s-]+"), "")
        if (requestedUpper != "CODE_128" && compact.isNotEmpty()) {
            val fallbackAttempt = tryEncode(compact, "CODE_128", widthPx, heightPx)
            if (fallbackAttempt != null) {
                return BarcodeRenderResult.Rendered(
                    bitmap = fallbackAttempt,
                    usedFormat = "CODE_128",
                    usedFallbackFormat = true
                )
            }
        }

        return BarcodeRenderResult.FallbackTextOnly(
            displayNumber = formatSpacedDisplayNumber(trimmed),
            reason = "Barcode format unavailable — show number to cashier"
        )
    }

    private fun tryEncode(
        content: String,
        formatName: String,
        widthPx: Int,
        heightPx: Int
    ): Bitmap? = runCatching {
        val zxingFormat = toZxingFormat(formatName)
        val isSquare = is2DFormat(formatName)
        val targetW = if (isSquare) minOf(widthPx, 640) else widthPx
        val targetH = if (isSquare) targetW else heightPx
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix: BitMatrix = MultiFormatWriter().encode(content, zxingFormat, targetW, targetH, hints)
        bitMatrixToBitmap(matrix)
    }.getOrNull()

    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
