package com.example.posdemo.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec

object UStageCrypto {

    private const val TRANSFORMATION = "DES/CBC/PKCS5Padding"
    private const val ALGORITHM = "DES"
    private const val DEFAULT_KEY = "12345678"
    private const val DEFAULT_IV = "12345678"
    private val UTF8: Charset = StandardCharsets.UTF_8

    // 原 App 用的分隔符：0x1E
    private const val SEGMENT_SEPARATOR_CHAR: Char = '\u001e'
    private const val SEGMENT_SEPARATOR_ESC = "\\u001e" // 文字形式的 \u001e（从日志/JSON拷贝经常会出现）

    /**
     * 等价逻辑（对齐你给的 Java）：
     * Deflater(zip, level=9) -> Base64(NO_PADDING) -> DES/CBC/PKCS5Padding -> Base64(NO_WRAP)
     */
    @JvmStatic
    fun zipAndEncrypt(plain: String, key8: String = DEFAULT_KEY, iv8: String = DEFAULT_IV): String {
        require(plain.isNotBlank()) { "明文不能为空" }
        require(key8.length == 8) { "密钥必须为8位" }
        require(iv8.length == 8) { "偏移量必须为8位" }

        val zippedBase64 = zipStringToBase64_noPadding_likeUStage(plain)
        return desEncryptToBase64_noWrap(zippedBase64, key8, iv8)
    }

    /**
     * 对应逆过程（单段密文）：
     * Base64(NO_WRAP) -> DES解密 -> (得到zipString的Base64文本) -> Base64(NO_PADDING) -> Inflater解压 -> 原文
     */
    @JvmStatic
    fun decryptAndUnzip(cipherText: String, key8: String = DEFAULT_KEY, iv8: String = DEFAULT_IV): String {
        require(cipherText.isNotBlank()) { "密文不能为空" }
        require(key8.length == 8) { "密钥必须为8位" }
        require(iv8.length == 8) { "偏移量必须为8位" }

        val normalized = normalizeMaybeJsonEscaped(cipherText)
        val zippedBase64 = desDecryptFromBase64_noWrap(normalized, key8, iv8)
        return unzipBase64ToString_noPadding_likeUStage(zippedBase64)
    }

    /**
     * ✅ 你现在最需要的：解密“多段密文拼接串”
     * - 输入可能包含：\/、\\u001e、真正的 \u001e
     * - 会按分隔符拆分，然后逐段 decryptAndUnzip
     */
    @JvmStatic
    fun decryptAndUnzipMulti(cipherTextMulti: String, key8: String = DEFAULT_KEY, iv8: String = DEFAULT_IV): List<String> {
        require(cipherTextMulti.isNotBlank()) { "密文不能为空" }
        require(key8.length == 8) { "密钥必须为8位" }
        require(iv8.length == 8) { "偏移量必须为8位" }

        val normalizedAll = normalizeMaybeJsonEscaped(cipherTextMulti)

        val parts = splitSegments(normalizedAll)
        require(parts.isNotEmpty()) { "未解析到任何段" }

        return parts.map { part ->
            decryptAndUnzip(part, key8, iv8)
        }
    }

    /**
     * 有些场景你只想要第一段（例如验证流程是否OK）
     */
    @JvmStatic
    fun decryptAndUnzipFirst(cipherTextMulti: String, key8: String = DEFAULT_KEY, iv8: String = DEFAULT_IV): String {
        val normalizedAll = normalizeMaybeJsonEscaped(cipherTextMulti)
        val first = splitSegments(normalizedAll).firstOrNull()
            ?: throw IllegalArgumentException("未解析到任何段")
        return decryptAndUnzip(first, key8, iv8)
    }

    // ---------------------------
    // internal helpers
    // ---------------------------

    /**
     * 处理从日志/JSON拷贝出来的字符串：
     * - 还原 \/ -> /
     * - 把文本 "\\u001e" 变成真正的分隔符 '\u001e'
     *
     * 注意：这里不做“去空白”，因为 base64 的空白处理在 decode 前更合适。
     */
    private fun normalizeMaybeJsonEscaped(s: String): String {
        var out = s.trim()

        // JSON里经常把 / 转义成 \/
        out = out.replace("\\/", "/")

        // 如果是文本形式的 "\u001e"（6个字符），转成真正分隔符
        out = out.replace(SEGMENT_SEPARATOR_ESC, SEGMENT_SEPARATOR_CHAR.toString(), ignoreCase = true)

        return out
    }

    private fun splitSegments(s: String): List<String> {
        // 既支持真正的 0x1E，也支持用户可能手动输入的 "\u001e"（已在 normalize 中转过了）
        return s.split(SEGMENT_SEPARATOR_CHAR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 对齐原 Java：Base64.encodeToString(..., 1) => NO_PADDING（但可能带换行）
     */
    private fun zipStringToBase64_noPadding_likeUStage(input: String): String {
        val deflater = Deflater(9)
        deflater.setInput(input.toByteArray(UTF8))
        deflater.finish()

        val buffer = ByteArray(256)
        val baos = ByteArrayOutputStream(256)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            baos.write(buffer, 0, count)
        }
        deflater.end()

        // flag=1 : NO_PADDING（保持与原版一致）
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_PADDING)
    }

    /**
     * 对齐原 Java：Base64.decode(str, 1) => NO_PADDING
     * 但为了鲁棒性：先去掉所有空白（因为 zipString 可能产生带换行的 base64）
     */
    private fun unzipBase64ToString_noPadding_likeUStage(base64Zipped: String): String {
        val normalized = base64Zipped.trim().replace("\\s+".toRegex(), "")
        val compressed = Base64.decode(normalized, Base64.NO_PADDING)
        require(compressed.isNotEmpty()) { "Base64解码结果为空" }

        val inflater = Inflater()
        inflater.setInput(compressed)

        val buffer = ByteArray(256)
        val baos = ByteArrayOutputStream(256)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer) // 可能抛 DataFormatException
            baos.write(buffer, 0, count)
        }
        inflater.end()

        return baos.toString(UTF8.name())
    }

    /**
     * 对齐原 Java：Base64.encodeToString(..., 2) => NO_WRAP
     */
    private fun desEncryptToBase64_noWrap(plain: String, key8: String, iv8: String): String {
        val keySpec = DESKeySpec(key8.toByteArray(UTF8))
        val secretKey = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec)
        val ivSpec = IvParameterSpec(iv8.toByteArray(UTF8))

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encrypted = cipher.doFinal(plain.toByteArray(UTF8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * 对齐原 Java：decode 用 NO_WRAP
     * 并且去掉所有空白（你的异常里就是 bad base-64，多半来自分隔符/反斜杠/空白）
     */
    private fun desDecryptFromBase64_noWrap(cipherText: String, key8: String, iv8: String): String {
        val normalized = cipherText.trim().replace("\\s+".toRegex(), "")
        val cipherBytes = Base64.decode(normalized, Base64.NO_WRAP)

        val keySpec = DESKeySpec(key8.toByteArray(UTF8))
        val secretKey = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec)
        val ivSpec = IvParameterSpec(iv8.toByteArray(UTF8))

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        val decrypted = cipher.doFinal(cipherBytes)
        return String(decrypted, UTF8) // 这里返回 zipString 的 Base64 文本（NO_PADDING，可能含换行）
    }
}