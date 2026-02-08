package com.example.posdemo.utils

import kotlin.random.Random

object DataUtil {

    fun toHexString(data: ByteArray?): String {
        if (data == null || data.isEmpty()) {
            return ""
        }

        val sb = StringBuilder(data.size * 3)
        for (b in data) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString().trim()
    }

    fun randomHex(bytes: Int): String {
        return ByteArray(bytes).also {
            Random.nextBytes(it)
        }.joinToString("") { "%02X".format(it) }
    }
}