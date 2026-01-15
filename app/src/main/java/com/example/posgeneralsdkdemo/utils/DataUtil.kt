package com.example.posgeneralsdkdemo.utils

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
}