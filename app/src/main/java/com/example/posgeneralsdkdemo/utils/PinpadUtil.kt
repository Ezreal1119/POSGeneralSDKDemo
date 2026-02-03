package com.example.posgeneralsdkdemo.utils

import android.content.Context
import android.device.SEManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.urovo.sdk.pinpad.PinPadProviderImpl
import com.urovo.sdk.pinpad.utils.Constant
import com.urovo.sdk.utils.BytesUtil
import com.urovo.sdk.utils.Funs

object PinpadUtil {

    fun getPinData(keyIndex: Int, pan: String, data: ByteArray, tag: String): String {
        var pinBlock = ""
        try {
            val iRet = PinPadProviderImpl.getInstance().calculateDes(
                Constant.DesMode.DEC,
                Constant.Algorithm.DES_ECB,
                Constant.KeyType.PIN_KEY,
                keyIndex,
                data,
                data
            )

            Log.e(tag, "calculateDes:$iRet")
            if (iRet != 0) {
                return pinBlock
            }

            Log.e(tag, "pinBlock:${Funs.bytesToHexString(data)}")

            var panStr = pan.substring(0, pan.length - 1)
            panStr = panStr.substring(panStr.length - 12)
            panStr = BytesUtil.FormatWithZero(panStr, "0000000000000000")

            Log.e(tag, "panStr:$panStr")

            val panBuff = Funs.StrToHexByte(panStr)

            do_xor_urovo(panBuff, data, 8)

            Log.e(tag, "pinBlock 2:${Funs.bytesToHexString(panBuff)}")

            pinBlock = Funs.bytesToHexString(panBuff)
            val pinLen = Integer.parseInt(pinBlock.substring(1, 2), 16)

            Log.e(tag, "Pin length:$pinLen")

            pinBlock = pinBlock.substring(2, 2 + pinLen)

            Log.e(tag, "clear pin:$pinBlock")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pinBlock
    }

    fun do_xor_urovo(src1: ByteArray, src2: ByteArray, num: Int) {
        for (i in 0 until num) {
            src1[i] = (src1[i].toInt() xor src2[i].toInt()).toByte()
        }
    }

    fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }


    /**
     * writeTR34Cert
     *
     * @param type      CA_TYPE_KMSCA,0xF2
     *                  CA_TYPE_PEDCRT,0xF3
     *                  CA_TYPE_KDHCRRT,0xF4
     *                  CA_TYPE_PEDPRV, 0xF5
     * @param index     0-3
     * @param data
     * @return
     */
    fun writeTr34Cert(type: Int, index: Int, data: ByteArray): Int {
        runCatching {
            SEManager().deleteTR34Cert(type, index)
            val ret = SEManager().writeTR34Cert(type, index, data, data.size)
            return ret
        }.onFailure {
            it.printStackTrace()
        }
        return -1
    }

    /**
     * readTR34Cert
     *
     * @param type         CA_TYPE_KMSCA,0xF2
     *                     CA_TYPE_PEDCRT,0xF3
     *                     CA_TYPE_KDHCRRT,0xF4
     *                     CA_TYPE_PEDPRV, 0xF5
     * @param index        0-3
     * @param responseData
     * @param resLen
     * @return
     */
    fun readTr34Cert(type: Int, index: Int, respData: ByteArray, respLen: IntArray): Int {
        runCatching {
            val ret = SEManager().readTR34Cert(type, index, respData, respLen)
            return ret
        }.onFailure {
            it.printStackTrace()
        }
        return -1
    }

    fun getJson(fileName: String, context: Context): String {
        runCatching {
            return context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.onFailure {
            it.printStackTrace()
        }
        return ""
    }

    fun getImageFromAssetsFile(context: Context, fileName: String): Bitmap? {
        val am = context.resources.assets
        runCatching {
            val inputStream = am.open(fileName)
            val image = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            return image
        }.onFailure {
            it.printStackTrace()
        }
        return null
    }

    fun parseTr31KeyUsage(tr31: String): String {
        if (tr31.length < 7) return "Invalid TR31"

        val usageCode = tr31.substring(5, 7)

        return when (usageCode) {
            "2B" -> "DUKPT BDK"
            "2A" -> "DUKPT IPEK"
            "P0" -> "PIN Encryption Key"
            "M0" -> "MAC Key"
            "D0" -> "Data Encryption Key"
            "K0" -> "Key Encrypting Key (KEK)"
            "B0", "B1" -> "Base Derivation Key"
            else -> "Unknown Key Usage ($usageCode)"
        }
    }

    fun parseTr31Algorithm(tr31: String): String {
        if (tr31.length < 8) return "Invalid TR31"

        val algCode = tr31[7]

        return when (algCode) {
            'A' -> "AES"
            'T' -> "Triple DES (TDES)"
            'D' -> "DES"
            'H' -> "HMAC"
            'R' -> "RSA"
            else -> "Unknown Algorithm ($algCode)"
        }
    }
}

enum class Tr34Type(val type: Int) {
    // KMS CA's private key is in Security Room
    TYPE_KMS_CA(0xF2), // The place that stores KMS CA, pre-embedded during production. Used to verify the KMS backend.
    TYPE_PED_CRT(0xF3), // RKI from KMS by CSR requesting to KMS server. Signed by KMS CA. Used to showcase the legit identity
    TYPE_KDH_CRT(0xF4), // Key Distribution Host (e.g. KLD). Also signed by KMS CA
    TYPE_PED_PRIVATE_KEY(0xF5)
}