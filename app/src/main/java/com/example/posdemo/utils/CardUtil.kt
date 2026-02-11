package com.example.posdemo.utils

import kotlin.random.Random

object CardUtil {
    fun parseCardType(code: Int): String {
        return when (code) {
            0x00 -> "S50_CARD"
            0x01 -> "S70_CARD"
            0x02 -> "PRO_CARD"
            0x03 -> "S50_PRO_CARD"
            0x04 -> "S70_PRO_CARD"
            0x05 -> "CPU_CARD"
            else -> "UNKNOWN CODE=$code"
        }
    }

    fun parseSak(sakDecimal: Int): String {
        val sak = sakDecimal and 0xFF
        val sb = StringBuilder()

        sb.appendLine("SAK: $sakDecimal (0x${sak.toString(16).uppercase().padStart(2, '0')})")

        // bit5 / bit6 → Card Type
        when {
            (sak and 0x20) != 0 ->
                sb.appendLine(" - APDU Card (CPU/EMV)")
            (sak and 0x40) != 0 ->
                sb.appendLine(" - MIFARE / Memory Card (NO CPU)")
            else ->
                sb.appendLine(" - Unknown / Low-level card")
        }

        // bit3 → UID status
        if ((sak and 0x08) != 0) {
            sb.appendLine(" - UID Cascade required (NOT complete)")
        } else {
            sb.appendLine(" - UID complete")
        }

        return sb.toString()
    }

    /**
     * Extract the first TLV value for the given tag from a hex string.
     *
     * Assumptions / simplified rules:
     * - Input is a hex string (may contain spaces/newlines).
     * - TLV format is: TAG + 1-byte LENGTH + VALUE
     * - LENGTH is exactly 1 byte (2 hex chars), i.e. short form only.
     * - Tag is provided as hex string (e.g. "4F", "9F38", "5F2D").
     * - This function uses indexOf(), so it does NOT guarantee TLV-safe parsing.
     *   (The tag sequence could appear inside another value.)
     *
     * @param hexString Full TLV hex string.
     * @param tag TLV tag in hex string.
     * @return Value hex string (without tag/length), or null if not found/invalid.
     */
    fun extractTlvValue(hexString: String, tag: String): String? {
        val s = hexString.replace("\\s".toRegex(), "").uppercase()
        val t = tag.replace("\\s".toRegex(), "").uppercase()

        if (t.isEmpty() || t.length % 2 != 0) return null

        val tagIndex = s.indexOf(t)
        if (tagIndex < 0) return null

        // Length starts immediately after tag
        val lenIndex = tagIndex + t.length
        if (lenIndex + 2 > s.length) return null

        // Parse 1-byte length
        val lenHex = s.substring(lenIndex, lenIndex + 2)
        val lenBytes = lenHex.toIntOrNull(16) ?: return null

        // Value starts after length
        val valueStart = lenIndex + 2
        val valueEnd = valueStart + lenBytes * 2
        if (valueEnd > s.length) return null

        return s.substring(valueStart, valueEnd)
    }

    /**
     * Extract the 1-byte TLV length (LEN) as a 2-hex-digit string
     * for the first occurrence of the given tag.
     *
     * Assumptions / simplified rules:
     * - TLV format is: TAG + 1-byte LENGTH + VALUE
     * - LENGTH is exactly 1 byte (2 hex chars), short form only.
     * - This function uses indexOf(), so it does NOT guarantee TLV-safe parsing.
     *
     * @param hexString Full TLV hex string.
     * @param tag TLV tag in hex string (e.g. "4F", "9F38").
     * @return LEN as 2-hex-digit string (e.g. "07"), or null if not found/invalid.
     */
    fun extractTlvLenHex(hexString: String, tag: String): String? {
        val s = hexString.replace("\\s".toRegex(), "").uppercase()
        val t = tag.replace("\\s".toRegex(), "").uppercase()

        if (t.isEmpty() || t.length % 2 != 0) return null

        val tagIndex = s.indexOf(t)
        if (tagIndex < 0) return null

        // Length starts immediately after tag
        val lenIndex = tagIndex + t.length
        if (lenIndex + 2 > s.length) return null

        val lenHex = s.substring(lenIndex, lenIndex + 2)

        // Validate hex format quickly (optional but safe)
        if (!lenHex.matches(Regex("^[0-9A-F]{2}$"))) return null

        return lenHex
    }

    /**
     * Calculate total required bytes for a PDOL (or any DOL) definition string.
     *
     * PDOL format: (Tag + 1-byte length) repeated.
     * Example: "9F7A019F02065F2A02DF6901"
     * Result: 1 + 6 + 2 + 1 = 10
     *
     * Simplified assumptions:
     * - Tag is 1 byte (2 hex chars) OR 2 bytes (4 hex chars).
     * - If the first tag byte is one of: 9F, 5F, DF -> treat tag as 2 bytes.
     * - Length is always 1 byte (2 hex chars).
     */
    fun pdolTotalBytes(pdolHex: String): Int {
        val s = pdolHex.replace("\\s".toRegex(), "").uppercase()
        var i = 0
        var total = 0

        while (i < s.length) {
            if (i + 2 > s.length) break

            val firstByte = s.substring(i, i + 2)
            val tagLenChars = if (firstByte == "9F" || firstByte == "5F" || firstByte == "DF") 4 else 2

            // Move past tag
            i += tagLenChars
            if (i + 2 > s.length) break

            // Read 1-byte length
            val lenHex = s.substring(i, i + 2)
            val len = lenHex.toInt(16)
            total += len

            // Move past length
            i += 2
        }

        return total
    }

    /**
     * Convert an Int to 1-byte hex string (2 hex digits, uppercase).
     * Example: 0   -> "00"
     *          10  -> "0A"
     *          255 -> "FF"
     */
    fun intTo1ByteHex(value: Int): String {
        require(value in 0..0xFF)
        return value.toString(16).uppercase().padStart(2, '0')
    }

    /**
     * Generate a random numeric string with given length.
     *
     * @param length Number of digits.
     * @return Random numeric string, e.g. "493817"
     */
    fun randomNumberString(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length * 2) {
            sb.append(Random.nextInt(0, 10))
        }
        return sb.toString()
    }
}