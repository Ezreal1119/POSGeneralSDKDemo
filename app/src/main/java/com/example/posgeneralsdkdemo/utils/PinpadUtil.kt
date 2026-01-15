package com.example.posgeneralsdkdemo.utils

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
}