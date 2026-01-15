package com.example.posgeneralsdkdemo

import android.content.Context
import android.device.SEManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.utils.DataUtil
import com.example.posgeneralsdkdemo.utils.DebugUtil
import com.example.posgeneralsdkdemo.utils.PinpadUtil
import com.urovo.sdk.pinpad.PinPadProviderImpl
import com.urovo.sdk.pinpad.listener.PinInputListener
import com.urovo.sdk.pinpad.utils.Constant
import com.urovo.sdk.utils.BytesUtil

/**
 * Author: Patrick
 * Date: 2025/12/17
 * Must note: The Encryption algorithm being used is DES(8Bytes)/TDES(16/24Bytes). AES is not used in this DEMO code.
 * keySlot_0 is for DUKPT(PIN, EMV(ICC/CL), MSR(MSC), MAC(ISO8583_DATA))
 * keySlot_[1, 255] are for others (TEK/MK/E_MK, TD_KEY, PIN_KEY, MAC_KEY)
 */

private const val TAG = "Pinpad_Patrick"
private const val INDEX_NINETY_NINE = 99
private const val TEK = "00000000000000000000000000000000" // 16 Bytes
private const val TEK_KCV = "8CA64DE9C1B123A7" // 8 Bytes

private const val MK = "11111111111111111111111111111111" // 16 Bytes
private const val E_MK = "89B07B35A1B3F47E89B07B35A1B3F47E" // // 16 Bytes
private const val MK_KCV = "82E13665B4624DF5" // 8 Bytes
// private const val PIN_KEY = "22222222222222222222222222222222" // 16 Bytes
private const val E_PIN_KEY = "950973182317F80B950973182317F80B" // 16 Bytes, encrypted from PIN_KEY
private const val PIN_KCV = "00962B60AA556E65" // 8 Bytes
// private const val TD_KEY = "44444444444444444444444444444444" // 16 Bytes
private const val E_TD_KEY = "A0C45C59F1E549BBA0C45C59F1E549BB" // 16 Bytes, encrypted from TD_KEY
private const val TD_KCV = "E2F2434039AA34AC" // 8 Bytes
// private const val MAC_KEY = "33333333333333334444444444444444" // 16 Bytes
private const val E_MAC_KEY = "F679786E2411E3DEA0C45C59F1E549BB" // 16 Bytes, encrypted from MAC_KEY
private const val MAC_KCV = "E18DE25ECBCF1591" // 8 Bytes

private const val ISO8583_DATA = "1200721405D820C0820116986009010120744800000000000001000020211104115855211104115855241200000101100020015065999211101001379860090101207448D24122011374015900000012733370041988888888028602869F2608FF852238242376749F2701809F10120114A74003020000000000000000000000FF9F370478D842739F360201C7950500800080009A032111049C01009F02060000000100005F2A020860820239009F1A0208609F03060000000000009F3303E0F0C89F34034403029F3501229F1E0830303030303030308407A08600010000019F090200209F41040000000443D0964F00000000"
private const val EMV_DATA = "9F260814E50F30268921459F2701409F1007060201039400029F3704A7D8F2329F36020601950500800000009A031901029B02E8009C0100"
private const val ENC_EMV_DATA_USING_FIRST_KSN = "6641306C68F18A9C705ACD79336141DF10E2E4C78BEE079C6C71F64185DAF7609D22A64D30902EE02BA7A846B840EF4D4E8D8AC849F1E696"
private const val MAC_ANSI_99 = "502A20C53785A8FB"
private const val MAC_ANSI_919 = "DD0106290E3A4B08"
private const val TRACK2_DATA = "621996044447640027D0506101152641"
private const val TRACK2_DATA_DES_ECB = "1F2570DB45E40261D323ADA0FB83DB870787547AACCFEBB7257C76AF088733DF"
private const val TRACK2_DATA_DES_CBC = "1F2570DB45E4026190C86844CB06EAA0A5F0C6845C0E0873DA31C229AE3D5FE4"

private const val RANDOM_NUMBER = "18956198561290728915719572156891565189658916589165259681256193565794"
private const val RANDOM_NUMBER_HASH = "4D84872A9699102D036F3CC8930F9332C7A8FE5DDD3A94F201FBFDA46BFE289"

// private const val BDK = "0123456789ABCDEFFEDCBA9876543210"
private const val KSN= "FFFF9876543210E00000"

private const val IPEK = "6AC292FAA1315B4D858AB3A3D7D5933A"
private const val PAN = "6217003810042210743"
private const val PAD_TITLE = "Patrick's Pin Pad"
private const val PAD_MESSAGE = "Please enter the PIN :)"
private const val SUPPORT_PIN_LENGTH = "4,6,12"
private const val TIMEOUT_MS = 30 * 1000L

class PinpadActivity : AppCompatActivity() {
    private val etKeySlot by lazy { findViewById<EditText>(R.id.etKeySlot) }
    private val btnIfKeysExist by lazy { findViewById<Button>(R.id.btnIfKeysExist) }
    private val btnDeleteKeys by lazy { findViewById<Button>(R.id.btnDeleteKeys) }
    private val btnDeleteAllKeys by lazy { findViewById<Button>(R.id.btnDeleteAllKeys) }
    private val btnLoadTEK by lazy { findViewById<Button>(R.id.btnLoadTEK) }
    private val btnLoadMK by lazy { findViewById<Button>(R.id.btnLoadMK) }
    private val btnLoadEMK by lazy { findViewById<Button>(R.id.btnLoadEMK) }
    private val btnLoadWK by lazy { findViewById<Button>(R.id.btnLoadWK) }
    private val btnCalcMac by lazy { findViewById<Button>(R.id.btnCalcMac) }
    private val btnEncDecData by lazy { findViewById<Button>(R.id.btnEncDecData) }
    private val btnCalcHash by lazy { findViewById<Button>(R.id.btnCalcHash) }
    private val btnPinBlockWKOnline by lazy { findViewById<Button>(R.id.btnPinBlockWKOnline) }
    private val btnPinBlockWKOffline by lazy { findViewById<Button>(R.id.btnPinBlockWKOffline) }
    private val btnDownloadDukpt by lazy { findViewById<Button>(R.id.btnDownloadDukpt) }
    private val btnDukptGetKSN by lazy { findViewById<Button>(R.id.btnDukptGetKSN) }
    private val btnDeleteDukpt by lazy { findViewById<Button>(R.id.btnDeleteDukpt) }
    private val btnCalcMacDukpt by lazy { findViewById<Button>(R.id.btnCalcMacDukpt) }
    private val btnEMVEnc_Dukpt by lazy { findViewById<Button>(R.id.btnEMVEnc_Dukpt) }
    private val btnEMVDec_Dukpt by lazy { findViewById<Button>(R.id.btnEMVDec_Dukpt) }
    private val btnPinBlockDukpt by lazy { findViewById<Button>(R.id.btnPinBlockDukpt) }
    private val btnPinBlockDukptCustom by lazy { findViewById<Button>(R.id.btnPinBlockDukptCustom) }
    private val btnClearAllKeys by lazy { findViewById<Button>(R.id.btnClearAllKeys) }
    private val pbWaiting by lazy { findViewById<ProgressBar>(R.id.pbWaiting) }
    private val tvIntro by lazy { findViewById<TextView>(R.id.tvIntro) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }

    private val mPinpadManager = PinPadProviderImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pinpad)

        btnIfKeysExist.setOnClickListener { onIfKeysExistButtonClicked() }
        btnDeleteKeys.setOnClickListener { onDeleteKeysButtonClicked() }
        btnDeleteAllKeys.setOnClickListener { onDeleteAllKeysButtonClicked() }
        btnLoadTEK.setOnClickListener { onLoadTEKButtonClicked() }
        btnLoadMK.setOnClickListener { onLoadMKButtonClicked() }
        btnLoadEMK.setOnClickListener { onLoadEMKButtonClicked() }
        btnLoadWK.setOnClickListener { onLoadWKButtonClicked() }
        btnCalcMac.setOnClickListener { onCalcMacButtonClicked() }
        btnEncDecData.setOnClickListener { onEncDecDataButtonClicked() }
        btnCalcHash.setOnClickListener { onCalcHashButtonClicked() }
        btnPinBlockWKOnline.setOnClickListener { onPinBlockWKOnlineButtonClicked() }
        btnPinBlockWKOffline.setOnClickListener { onPinBlockWKOfflineButtonClicked() }
        btnDownloadDukpt.setOnClickListener { onDownloadDukptButtonClicked() }
        btnDukptGetKSN.setOnClickListener { onDukptGetKSNButtonClicked() }
        btnDeleteDukpt.setOnClickListener { onDeleteDukptButtonClicked() }
        btnCalcMacDukpt.setOnClickListener { onCalcMacDukptButtonClicked() }
        btnEMVEnc_Dukpt.setOnClickListener { onEMVEncDukptButtonClicked() }
        btnEMVDec_Dukpt.setOnClickListener { onEMVDecDukptButtonClicked() }
        btnPinBlockDukpt.setOnClickListener { onPinBlockDukptButtonClicked() }
        btnPinBlockDukptCustom.setOnClickListener { onPinBlockDukptCustomButtonClicked() }
        btnClearAllKeys.setOnClickListener { onClearAllKeysButtonClicked() }
    }

    // ----------------------------------------- //

    private fun onIfKeysExistButtonClicked() {
        // Check if a keySlot has these four types of keys or not. (Each slot can have this four types of keys, but only of its kind)
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        var checkMK= false
        var checkTDKey= false
        var checkPinKey= false
        var checkMacKey= false
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            checkMK = mPinpadManager.isKeyExist(Constant.KeyType.MAIN_KEY, keySlot)
            checkTDKey = mPinpadManager.isKeyExist(Constant.KeyType.TD_KEY, keySlot)
            checkPinKey = mPinpadManager.isKeyExist(Constant.KeyType.PIN_KEY, keySlot)
            checkMacKey = mPinpadManager.isKeyExist(Constant.KeyType.MAC_KEY, keySlot)
        }.onSuccess {
            tvResult.text = buildString {
                append("Check MK/SK results:\n")
                append(" - TEK/MK(TMK) @ KeySlot=$keySlot: ${checkMK}\n")
                append(" - TD_Key @ KeySlot=$keySlot: ${checkTDKey}\n")
                append(" - PIN_Key(TPK) @ KeySlot=$keySlot: ${checkPinKey}\n")
                append(" - MAC_Key @ KeySlot=$keySlot: ${checkMacKey}\n\n")
                append("Please Note: \n")
                append("Check if a keySlot[0, 255] has these four types of keys or not. (Each slot can have this four types of keys, but only of its kind)")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onDeleteKeysButtonClicked() {
        // Delete all keys(TEK/TMK, TD_KEY, PIN_KEY, MAC_KEY) of specified keySlot
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            listOf(
                Constant.KeyType.MAIN_KEY,
                Constant.KeyType.TD_KEY,
                Constant.KeyType.PIN_KEY,
                Constant.KeyType.MAC_KEY,
                Constant.KeyType.ENCDEC_KEY
            ).forEach { type ->
                mPinpadManager.deleteKey(type, keySlot)
            }
        }.onSuccess {
            tvResult.text = buildString {
                append("Deleted all the Keys successfully:\n")
                append(" - TEK / MK(TMK) @ KeySlot=$keySlot\n")
                append(" - TD_KEY @ KeySlot=$keySlot\n")
                append(" - PIN_KEY @ KeySlot=$keySlot\n")
                append(" - MAC_KEY @ KeySlot=$keySlot")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onDeleteAllKeysButtonClicked() {
        // Delete all Keys(TEK/TMK, TD_KEY, PIN_KEY, MAC_KEY) of all the keySlot [1, 255]
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure to delete all the Keys(MK/SK) from Slot 1 - 255")
            .setPositiveButton("Delete") { dialog, which ->
                pbWaiting.visibility = View.VISIBLE
                tvIntro.visibility = View.INVISIBLE
                Thread {
                    runCatching {
                        for (i in 1..255) {
                            listOf(
                                Constant.KeyType.MAIN_KEY,
                                Constant.KeyType.TD_KEY,
                                Constant.KeyType.PIN_KEY,
                                Constant.KeyType.MAC_KEY,
                                Constant.KeyType.ENCDEC_KEY
                            ).forEach { type ->
                                mPinpadManager.deleteKey(type, i)
                            }
                        }
                    }.onSuccess {
                        runOnUiThread {
                            tvResult.text = buildString {
                                append("Deleted all the Keys successfully:\n")
                                append(" - TEK / MK(TMK) from KeySlot=[1, 255]\n")
                                append(" - TD_KEY from KeySlot=[1, 255]\n")
                                append(" - PIN_KEY from KeySlot=[1, 255]\n")
                                append(" - MAC_KEY from KeySlot=[1, 255]")
                            }

                        }
                    }.onFailure {
                        runOnUiThread {
                            tvResult.text = it.message
                        }
                        it.printStackTrace()
                    }
                    runOnUiThread {
                        pbWaiting.visibility = View.INVISIBLE
                        tvIntro.visibility = View.VISIBLE
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onLoadTEKButtonClicked() {
        // Load plaintext TEK into the device; FYI, TEK is used to encrypt the MK to E_MK out of device, which will be decrypted using the same TEK during injection.
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            val checkValue = BytesUtil.hexString2Bytes(TEK_KCV)
            val plainTEKBuffer = BytesUtil.hexString2Bytes(TEK)
            val result = mPinpadManager.loadTEK(keySlot, plainTEKBuffer, checkValue)
            if (!result) throw Exception("Loaded TEK failed")
        }.onSuccess {
            tvResult.text = buildString {
                append("TEK(Plain) loaded successfully!\n")
                append("TEK(Plain): $TEK\n\n")
                append("Please note:\n")
                append("TEK(Terminal Encryption Key) is used to decrypt the encrypted MK(Main Key) to be loaded into the device.")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onLoadMKButtonClicked() {
        // Load plaintext MK into the device; FYI, MK is used to encrypt the WK out of device to E_WK, which will be decrypted using the same MK during injection.
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            val checkValue = BytesUtil.hexString2Bytes(MK_KCV)
            val plainMKBuffer = BytesUtil.hexString2Bytes(MK)
            val result = mPinpadManager.loadMainKey(keySlot, plainMKBuffer, checkValue)
            if (!result) throw Exception("Loaded Plain MK(TMK) failed")
        }.onSuccess {
            tvResult.text = buildString {
                append("Plain MK(TMK) loaded successfully!\n")
                append("Plain MK(TMK): $MK\n\n")
                append("Please note:\n")
                append("MK(Main Key) is used to decrypt the encrypted WK(Work Key) to be loaded into the device.")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onLoadEMKButtonClicked() {
        // Load encrypted MK into the device; Device will use the same TEK inside for decryption of the E_MK
        // It's using DES(ECB/CBC mode) as the decryption algorithm.
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        val encryptedMK = ByteArray(BytesUtil.hexString2Bytes(MK).size)
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            val retEncryption = mPinpadManager.calculateDes(Constant.DesMode.ENC, Constant.Algorithm.DES_ECB, Constant.KeyType.MAIN_KEY, keySlot,
                BytesUtil.hexString2Bytes(MK), encryptedMK)
            if (retEncryption != 0x00) throw Exception("MK Encryption failed")
            val checkValue = BytesUtil.hexString2Bytes(MK_KCV)
            val result =
                mPinpadManager.loadEncryptMainKey(keySlot, keySlot, encryptedMK, checkValue)
            if (!result) throw Exception("Loaded Encrypted_MK(EMK) failed")
        }.onSuccess {
            tvResult.text = buildString {
                append("EMK loaded successfully! \n")
                append("Encrypted_MK: ${BytesUtil.bytes2HexString(encryptedMK)}\n")
                append("(Encrypted from MK, using calculateDes(); Excepted:\n$E_MK)\n\n")
                append("(Using the TEK of the same slot for encryption. Can be changed, will failed if no have TEK)\n")
                append("Means: EMK ---<TEK>---> MK \n\n")
                append("Please note:\n")
                append("EMK(Encrypted Main Key) will be decrypted by TEK and loaded into the device. And be used to encrypt WK(Work Key) in the future.")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onLoadWKButtonClicked() {
        // Load encrypted WK(TD_KEY, PIN_KEY, MAC_KEY) into device. Device will use the same MK for decryption.
        // It's using DES(ECB/CBC mode) as the decryption algorithm.
        val keySlot = etKeySlot.text.toString().trim().toIntOrNull()
        var retTDKEy = false
        var retPINKey = false
        var retMACKey = false
        runCatching {
            if (keySlot == null || keySlot !in 1..255) throw Exception("Please enter an integer between 1 and 255")
            retTDKEy = mPinpadManager.loadWorkKey(
                Constant.KeyType.TD_KEY, keySlot, keySlot,
                BytesUtil.hexString2Bytes(E_TD_KEY), BytesUtil.hexString2Bytes(TD_KCV)
            )
            retPINKey = mPinpadManager.loadWorkKey(
                Constant.KeyType.PIN_KEY, keySlot, keySlot,
                BytesUtil.hexString2Bytes(E_PIN_KEY), BytesUtil.hexString2Bytes(PIN_KCV)
            )
            retMACKey = mPinpadManager.loadWorkKey(
                Constant.KeyType.MAC_KEY, keySlot, keySlot,
                BytesUtil.hexString2Bytes(E_MAC_KEY), BytesUtil.hexString2Bytes(MAC_KCV)
            )
        }.onSuccess {
        tvResult.text = buildString {
            append("TD_KEY load result: $retTDKEy\n")
            append("Please note: TD_KEY is used to encrypt Transaction Data\n\n")
            append("PIN_KEY load result: $retPINKey\n")
            append("Please note: PIN_KEY is used to encrypt PIN Data\n\n")
            append("MAC_KEY load result: $retMACKey\n")
            append("Please note: MAC_KEY is used to encrypt calculated MAC Data\n\n")
            append("-> If loaded fail, might because no MK in the device")
        }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCalcMacButtonClicked() {
        // Calculate MAC using given ISO8583_DATA with different algorithms.
        // MAC_Key at Slot 99 is being used for the encryption; It's using DES(ECB/CBC mode) as the encryption algorithm.
        runCatching {
            val macX99 = DataUtil.toHexString(
                mPinpadManager.calcMAC(
                    INDEX_NINETY_NINE,
                    BytesUtil.hexString2Bytes(ISO8583_DATA),
                    0x01
                )
            )
            val macX919 = DataUtil.toHexString(
                mPinpadManager.calcMAC(
                    INDEX_NINETY_NINE,
                    BytesUtil.hexString2Bytes(ISO8583_DATA),
                    0x11
                )
            )
            Pair(macX99, macX919)
        }.onSuccess { (macX99, macX919) ->
            val result = buildString {
                append("Using MAC_Key at keySlot_99 for encryption:\n")
                append("MAC(ANSI X9.9): \n")
                append(macX99)
                append("\nExpected: \n$MAC_ANSI_99\n\n")

                append("MAC(ANSI X9.19): \n")
                append(macX919)
                append("\nExpected: \n$MAC_ANSI_919\n\n")
                append("ISO8583_DATA: \n$ISO8583_DATA")
            }
            tvResult.text = result
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onEncDecDataButtonClicked() {
        // To encrypt a piece of data(e.g., TRACK_2) using TD_KEY
        // TD_Key at Slot 99 is being used for encryption
        // Note: It's using TD_Key for Enc&Dec; It's using DES(ECB/CBC mode) as the encryption algorithm.
        val trackDataInBytes = TRACK2_DATA.toByteArray(Charsets.US_ASCII)
        val expectedDesEncEncryptedData = BytesUtil.hexString2Bytes(TRACK2_DATA_DES_ECB)
        val expectedDesCbcEncryptedData = BytesUtil.hexString2Bytes(TRACK2_DATA_DES_CBC)
        val outputDesEcb_enc = ByteArray(trackDataInBytes.size)
        var retDesEcb_enc = 0
        val outputDesCbc_enc = ByteArray(trackDataInBytes.size)
        var retDesCbc_enc = 0
        val outputDesEcb_dec = ByteArray(trackDataInBytes.size)
        var retDesEcb_dec = 0
        val outputDesCbc_dec = ByteArray(trackDataInBytes.size)
        var retDesCbc_dec = 0

        runCatching {

            retDesEcb_enc = mPinpadManager.calculateDes(
                Constant.DesMode.ENC,
                Constant.Algorithm.DES_ECB,
                Constant.KeyType.TD_KEY,
                INDEX_NINETY_NINE,
                trackDataInBytes,
                outputDesEcb_enc
            )

            retDesCbc_enc = mPinpadManager.calculateDes(
                Constant.DesMode.ENC,
                Constant.Algorithm.DES_CBC,
                Constant.KeyType.TD_KEY,
                INDEX_NINETY_NINE,
                trackDataInBytes,
                outputDesCbc_enc
            )

            retDesEcb_dec = mPinpadManager.calculateDes(
                Constant.DesMode.DEC,
                Constant.Algorithm.DES_ECB,
                Constant.KeyType.TD_KEY,
                INDEX_NINETY_NINE,
                expectedDesEncEncryptedData,
                outputDesEcb_dec
            )

            retDesCbc_dec = mPinpadManager.calculateDes(
                Constant.DesMode.DEC,
                Constant.Algorithm.DES_CBC,
                Constant.KeyType.TD_KEY,
                INDEX_NINETY_NINE,
                expectedDesCbcEncryptedData,
                outputDesCbc_dec
            )


        }.onSuccess {
            var data: String
            tvResult.text = buildString {
                append("Using TD_Key at keySlot_99 for encryption:\n")
                append("Track 2: \n$TRACK2_DATA\n\n")
                if (retDesEcb_enc == 0x00) {
                    append("Track 2(DES_ECB encrypted): \n${DataUtil.toHexString(outputDesEcb_enc)}")
                } else {
                    append("Track 2(DES_ECB encrypted): \nERROR - [ret != 0 when DES_ECB]")
                }
                append("\nExpected: $TRACK2_DATA_DES_ECB\n")
                if (retDesEcb_dec == 0x00) {
                    data = String(outputDesEcb_dec, Charsets.US_ASCII)
                    append("Track 2(DES_ECB decrypted): \n$data")
                } else {
                    append("Track 2(DES_ECB decrypted): \nERROR - [ret != 0 when DES_ECB]")
                }

                if (retDesCbc_enc == 0x00) {
                    data = DataUtil.toHexString(outputDesCbc_enc)
                    append("\n\nTrack 2(DES_CBC encrypted): \n$data")
                } else {
                    append("\n\nTrack 2(DES_CBC encrypted): \nERROR - [ret != 0 when DES_CBC]")
                }
                append("\nExpected: $TRACK2_DATA_DES_CBC")
                if (retDesCbc_dec == 0x00) {
                    data = String(outputDesCbc_dec, Charsets.US_ASCII)
                    append("\nTrack 2(DES_CBC decrypted): \n$data")
                } else {
                    append("\nTrack 2(DES_CBC decrypted): \nERROR - [ret != 0 when DES_CBC]")
                }
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onCalcHashButtonClicked() {
        // The HashCode is generated using SHA(256) with PIN_KEY at Slot_99
        val randomNumber = BytesUtil.hexString2Bytes(RANDOM_NUMBER)
        val respData = ByteArray(64)
        val respLen = ByteArray(1)
        runCatching {
            val ret = mPinpadManager.genKeyHashValue(
                Constant.KeyType.PIN_KEY,
                INDEX_NINETY_NINE,
                randomNumber,
                randomNumber.size,
                respData,
                respLen
            )
            if (ret != 0x00) throw Exception("Error when calculating HashCode")
            return@runCatching respData.copyOf(respLen[0].toInt())
        }.onSuccess { result ->
            tvResult.text = buildString {
                append("Result: \n${BytesUtil.bytes2HexString(result)}\n")
                append("Expected: $RANDOM_NUMBER_HASH")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onPinBlockWKOnlineButtonClicked() {
        // This is Absolutely Online PIN. Thus, PINBlock is a must, PIN_KEY is a must.
        val pinpadBundle = Bundle().apply {
            putString(PinParams.CARD_NO.value, PAN) // The field is a Must for generating PINBlock
            putString(PinParams.TITLE.value, PAD_TITLE) // "" by default
            putString(PinParams.MESSAGE.value, PAD_MESSAGE) // "" by default
            putBoolean(PinParams.ONLINE_PIN.value, true) // MK/SK PIN Pad for Online PIN
            putBoolean(PinParams.SOUND.value, false) // Sound will be turned on when using the PinPad (lasting effect); "false" by default
            putBoolean(PinParams.BYPASS.value, false) // Support 0 PIN or not. false by default.
            putString(PinParams.SUPPORT_PIN_LEN.value, SUPPORT_PIN_LENGTH) // Will use the one set by last time by default. Thus, must set before using.
            putBoolean(PinParams.FULL_SCREEN.value, true) // true by default. Won't have Cancel button when half screen
            putLong(PinParams.TIMEOUT_MS.value, TIMEOUT_MS) // Time out since opening the Pad. 0 by default, must set!
            putBoolean(PinParams.RANDOM_KEYBOARD.value, false) // true by default.
            putInt(PinParams.PIN_KEY_NO.value, INDEX_NINETY_NINE) // The keySlot of the PIN_KEY to use for encryption. Must Set since onlinePin must be encrypted!
        }
        runCatching {
            mPinpadManager.getPinBlockEx(pinpadBundle, mPinInputListener)
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPinBlockWKOfflineButtonClicked() {
        Toast.makeText(this, "ERROR! Not yet implemented!", Toast.LENGTH_SHORT).show()
//        val pinpadBundle = Bundle().apply {
//            putString(PinParams.TITLE.value, PAD_TITLE)
//            putString(PinParams.MESSAGE.value, PAD_MESSAGE)
//            putBoolean(PinParams.SOUND.value, false)
//            putBoolean(PinParams.BYPASS.value, false)
//            putString(PinParams.SUPPORT_PIN_LEN.value, SUPPORT_PIN_LENGTH)
//            putBoolean(PinParams.FULL_SCREEN.value, true)
//            putLong(PinParams.TIMEOUT_MS.value, TIMEOUT_MS)
//            putBoolean(PinParams.RANDOM_KEYBOARD.value, false)
//
//            putString("Module", "C6A13B3784C6F98D8A5D8E0C88B8F2C7E0D7F5C0F3A7B69C4E5C9A3B1F9D8C1BA5D8E4F2C7A9B1D3E6F8C0A7D9B5E3F14A7C8D9E0F1B2C3D4E5F60718293A4B5")
//            putInt("ModeleLe", "C6A13B3784C6F98D8A5D8E0C88B8F2C7E0D7F5C0F3A7B69C4E5C9A3B1F9D8C1BA5D8E4F2C7A9B1D3E6F8C0A7D9B5E3F14A7C8D9E0F1B2C3D4E5F60718293A4B5".length)
//            putString("Exponent", "010001")
//            putInt("ExponentLen", 6)
//            putInt(PinParams.INPUT_TYPE.value, 4)
//        }
//        mPinpadManager.getOfflinePinBlock(pinpadBundle, object: OfflinePinInputListener {
//            override fun onInput(pinLen: Int, keyValue: Int) {
//                Log.e(TAG, "onInput: {pinLen=$pinLen, keyValue=$keyValue")
//            }
//
//            override fun onConfirm(resultCode: Int) {
//                tvResult.text = "onConfirm - Result Code: $resultCode"
//            }
//
//            override fun onCancel(errorCode: Int) {
//                Log.e(TAG, "onCancel: {errorCode=$errorCode}")
//            }
//
//            override fun onTimeOut(errorCode: Int) {
//                Log.e(TAG, "onTimeOut: {errorCode=$errorCode}")
//            }
//
//            override fun onError(errorCode: Int) {
//                Log.e(TAG, "onError: {errorCode=$errorCode}")
//            }
//
//            override fun onRetry(pinEntryType: Int, retryTimes: Int) {
//                Log.e(TAG, "onRetry: {pinEntryType=$pinEntryType, retryTimes=$retryTimes}")
//            }
//        })
    }

    private fun onDownloadDukptButtonClicked() {
        // Download the Dukpt_01(00 is for IPEK) into device (Four types together)
        pbWaiting.visibility = View.VISIBLE
        tvIntro.visibility = View.INVISIBLE
        tvResult.text = ""
        btnDownloadDukpt.isEnabled = false
        Thread {
            runCatching {
                val result = buildString {
                    for (key in Dukpt.entries) {
                        val bytesIpek = BytesUtil.hexString2Bytes(IPEK)
                        val bytesKsn = BytesUtil.hexString2Bytes(KSN)
                        val ret = mPinpadManager.downloadKeyDukpt(
                            key.index, null,
                            0,
                            bytesKsn,
                            bytesKsn.size,
                            bytesIpek,
                            bytesIpek.size
                        )
                        if (ret == 0x00) {
                            append("Dukpt ${key.name} Downloaded successfully!\n")
                            append(" - KSN: $KSN\n")
                        } else {
                            Log.e(TAG, "Download Dukpt failed: ret = $ret")
                            append("Dupkt ${key.name} Downloaded failed!\n")
                            append(" - null\n")
                        }
                    }
                    append("\nNote:\n")
                    append("The Dukpt keys downloaded were IPEKs(KSN counter: 000000)\n")
                    append("The difference from MK/SK is that it's unique per encryption/decryption")
                }
                return@runCatching result
            }.onSuccess { result ->
                runOnUiThread {
                    pbWaiting.visibility = View.INVISIBLE
                    tvIntro.visibility = View.VISIBLE
                    tvResult.text = result
                    btnDownloadDukpt.isEnabled = true
                }
            }.onFailure {
                runOnUiThread { tvResult.text = it.message }
                tvResult.text = it.message
                it.printStackTrace()
            }
        }.start()

    }

    private fun onDukptGetKSNButtonClicked() {
        // Check the current KSN Counter for four types of Dukpt
        // The KSN Counter will increment as per each transaction
        val ksnBuffer = ByteArray(10)
        runCatching {
            val result = buildString {
                for (key in Dukpt.entries) {
                    val ret = mPinpadManager.DukptGetKsn(key.index, ksnBuffer)
                    if (ret == 0x00) {
                        append("Dukpt ${key.name} KSN:\n")
                        append(" - ${BytesUtil.bytes2HexString(ksnBuffer)}\n")
                    } else {
                        append("Dukpt ${key.name} getKsn failed\n")
                        append(" - null\n")
                    }
                }
            }
            return@runCatching result
        }.onSuccess { result ->
            tvResult.text = result
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onDeleteDukptButtonClicked() {
        runCatching {
            SEManager().DukptDeleteKey(Dukpt.MSR.index)
            SEManager().DukptDeleteKey(Dukpt.EMV.index)
            SEManager().DukptDeleteKey(Dukpt.PIN.index)
            SEManager().DukptDeleteKey(Dukpt.MAC.index)
        }.onSuccess {
            tvResult.text = ""
            Toast.makeText(this, "Deleted All Dukpt successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }



    private fun onCalcMacDukptButtonClicked() {
//        // Calculate MAC using given ISO8583_DATA with different algorithms.
//        // MAC_Key at Slot 99 is being used for the encryption; It's using DES(ECB/CBC mode) as the encryption algorithm.
        val iso8583Data = BytesUtil.hexString2Bytes(ISO8583_DATA)
        val output = ByteArray(iso8583Data.size)
        val outputLen = IntArray(2)
        val ksnBuffer = ByteArray(10)
        val ksnLen = IntArray(2)
        var outputMac = ByteArray(0)
        runCatching {
            val ret = mPinpadManager.calculateMACOfDUKPTExtend(
                Dukpt.MAC.index,
                iso8583Data,
                iso8583Data.size,
                output,
                outputLen,
                ksnBuffer,
                ksnLen
            )
            if (ret != 0x00) throw Exception("Error on calculating MAC using MAC_DUKPT")
            outputMac = ByteArray(outputLen[0])
            System.arraycopy(output, 0, outputMac, 0, outputMac.size)
        }.onSuccess {
            tvResult.text = buildString {
                append("Using MAC_DUKPT for encryption:\n")
                append("MAC: ${BytesUtil.bytes2HexString(outputMac)} (Calculated from ISO8583_DATA using DUKPT_MAC)\n")
                append("KSN_MAC_DUPKT: ${BytesUtil.bytes2HexString(ksnBuffer)}")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onEMVEncDukptButtonClicked() {
        // Use EMV_DUKPT to encrypt EMV_DATA
        val emvData = BytesUtil.hexString2Bytes(EMV_DATA)
        val output = ByteArray(emvData.size)
        val outputLen = IntArray(2)
        val ksnBuffer = ByteArray(10)
        val ksnLen = IntArray(2)
        runCatching {
            val ret = mPinpadManager.DukptEncryptDataIV(
                0x03,
                Dukpt.EMV.index,
                0x00,
                ByteArray(8),
                8,
                emvData,
                emvData.size,
                output,
                outputLen,
                ksnBuffer,
                ksnLen
            )
            if (ret != 0x00) throw Exception("onEMVEncryptDupktButtonClicked: enc_ret=$ret")
        }.onSuccess {
            tvResult.text = buildString {
                append("Enc result: ${BytesUtil.bytes2HexString(output)}\n")
                append("KSN_EMV_DUKPT: ${BytesUtil.bytes2HexString(ksnBuffer)}\n\n")
                append("Note:\n")
                append("It's using DUKPT_EMV to encrypt EMV_DATA\n")
                append("Original EMV_DATA: $EMV_DATA")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onEMVDecDukptButtonClicked() {
        // Use EMV_DUKPT to decrypt EMV_DATA
        val encryptedEmvData = BytesUtil.hexString2Bytes(ENC_EMV_DATA_USING_FIRST_KSN)
        val output = ByteArray(encryptedEmvData.size)
        val outputLen = IntArray(2)
        val ksnBuffer = ByteArray(10)
        val ksnLen = IntArray(2)
        runCatching {
            val ret = mPinpadManager.DukptEncryptDataIV(
                0x03,
                Dukpt.EMV.index,
                0x10,
                ByteArray(8),
                8,
                encryptedEmvData,
                encryptedEmvData.size,
                output,
                outputLen,
                ksnBuffer,
                ksnLen
            )
            if (ret != 0) throw Exception("onEMVEncryptDupktButtonClicked: dec_ret=$ret")
        }.onSuccess {
            tvResult.text = buildString {
                append("Dec result: \n${BytesUtil.bytes2HexString(output)}\n")
                append("KSN_EMV_DUKPT: ${BytesUtil.bytes2HexString(ksnBuffer)}\n\n")
                append("Please note, if using first KSN for encryption, the excepted result is: \n")
                append("$EMV_DATA")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onPinBlockDukptButtonClicked() {
        // This is Absolutely Online PIN. Thus,PINBlock is a must, DUKPT is a must.
        val pinpadBundle = Bundle().apply {
            putString(PinParams.CARD_NO.value, PAN) // The field is a Must for generating PINBlock
            putString(PinParams.TITLE.value, PAD_TITLE) // "" by default
            putString(PinParams.MESSAGE.value, PAD_MESSAGE) // "" by default
            putBoolean(PinParams.ONLINE_PIN.value, true) // Dukpt PIN Pad for Online PIN
            putBoolean(PinParams.SOUND.value, false) // Sound will be turned on when using the PinPad (lasting effect); "false" by default
            putBoolean(PinParams.BYPASS.value, false) // Support 0 PIN or not. false by default.
            putString(PinParams.SUPPORT_PIN_LEN.value, SUPPORT_PIN_LENGTH) // Will use the one set by last time by default. Thus, must set before using.
            putBoolean(PinParams.FULL_SCREEN.value, true) // true by default. Won't have Cancel button when half screen
            putLong(PinParams.TIMEOUT_MS.value, TIMEOUT_MS) // Time out since opening the Pad. 0 by default, must set!
            putBoolean(PinParams.RANDOM_KEYBOARD.value, false) // true by default.
            putInt(PinParams.PIN_KEY_NO.value, Dukpt.PIN.index) // Must set, will call onError otherwise
        }
        runCatching {
            mPinpadManager.GetDukptPinBlock(pinpadBundle, mPinInputListener)
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPinBlockDukptCustomButtonClicked() {
        val cancelBitmap = getImageFromAssetsFile(this@PinpadActivity, "cancel_butt_off.png")
        val delBitmap = getImageFromAssetsFile(this@PinpadActivity, "delete_butt_off.png")
        val okBitemap = getImageFromAssetsFile(this@PinpadActivity, "ok_butt_off.png")
        val backspaceBitmap = getImageFromAssetsFile(this@PinpadActivity, "back_white.png.png")
        val imageViewBitmap = getImageFromAssetsFile(this@PinpadActivity, "lock_art.png")
        val bodyBitmap = getImageFromAssetsFile(this@PinpadActivity, "bg_720x1280.png")
        val strJson = getJson("json_custom4_720x1280.json", this@PinpadActivity)
        val backgroundColor = intArrayOf(0X00FFFFFF, 0X00FFFFFF, 0X00FFFFFF, 0X00e3452f, 0X00895623, 0X00258945, 0X00364952, 0XFF123456.toInt(), 0XFF876328.toInt(), 0X00FFFFFF, 0XFF877454.toInt(), 0X00FFFFFF, 0xff1234FF.toInt(), 0X001c1c1c)
        val textColor = intArrayOf(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE)

        val pinpadBundle = Bundle().apply {
            putString(PinParams.CARD_NO.value, PAN) // The field is a Must for generating PINBlock
            putString(PinParams.TITLE.value, PAD_TITLE) // "" by default
            putString(PinParams.MESSAGE.value, PAD_MESSAGE) // "" by default
            putBoolean(PinParams.ONLINE_PIN.value, true) // Custom Dukpt PinPad for Online PIN
            putBoolean(PinParams.SOUND.value, true) // Sound will be turned on when using the PinPad (lasting effect); "false" by default
            putBoolean(PinParams.BYPASS.value, false) // Support 0 PIN or not. false by default.
            putString(PinParams.SUPPORT_PIN_LEN.value, SUPPORT_PIN_LENGTH) // Will use the one set by last time by default. Thus, must set before using.
            putBoolean(PinParams.FULL_SCREEN.value, true) // true by default. Won't have Cancel button when half screen
            putLong(PinParams.TIMEOUT_MS.value, TIMEOUT_MS) // Time out since opening the Pad. 0 by default, must set!
            putBoolean(PinParams.RANDOM_KEYBOARD.value, false) // true by default.
            putInt(PinParams.PIN_KEY_NO.value, Dukpt.PIN.index) // The keySlot of the PIN_KEY to use for encryption. Must Set since onlinePin must be encrypted!

            putIntArray("backgroundColor", backgroundColor)
            putIntArray("textColor", textColor)
            putBoolean("inputBySP", false)
            putString("infoLocation", "RIGHT")
            putBoolean("randomKeyboard", false)
            putBoolean("randomKeyboardLocation", false)
            putBoolean("customization", true)
            putString("strJson", strJson)
            putParcelable("cancelBitmap", cancelBitmap)
            putParcelable("delBitmap", delBitmap)
            putParcelable("okBitmap", okBitemap)
            putParcelable("backspaceBitmap", backspaceBitmap)
            putParcelable("viewBitmap", imageViewBitmap)
            putParcelable("bodyBitmap", bodyBitmap)
        }

        runCatching {
            mPinpadManager.GetDukptPinBlock(pinpadBundle, mPinInputListener)
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onClearAllKeysButtonClicked() {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure to delete all the keys(DUPKT + MK/SK)?")
            .setPositiveButton("Delete") { dialog, which ->
                runCatching {
                    val rspData = ByteArray(16)
                    val rspLen = ByteArray(2)
                    val ret = SEManager().clearKey(rspData, rspLen)
                    if (ret != 0) throw Exception("Clear all Keys(DUKPT+MK/SK) failed")
                    Triple(rspData, rspLen, ret)
                }.onSuccess { (rspData, rspLen, ret) ->
                    tvResult.text = buildString {
                        append("Clear all Keys successfully:\n")
                        append(" - MK/SK(TEK/MK, TD_KEY, PIN_KEY, MAC_KEY) from KeySlot=[1, 255]\n")
                        append(" - DUKPT(MSR, EMV, PIN, MAC)\n\n")
                        append("onSuccess: ret=$ret\n")
                        append("rspData: len=${rspLen[0]}\n")
                        append(BytesUtil.bytes2HexString(rspData.copyOf(rspLen[0].toInt())))
                    }
                }.onFailure {
                    tvResult.text = it.message
                    it.printStackTrace()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val mPinInputListener = object: PinInputListener {

        override fun onInput(pinLen: Int, keyValue: Int) {
            // This will be called whenever a number button is pressed
            // KeyValue will not be exposed, so all keyValues are "2"
            Log.e(TAG, "onInput: {pinLen=$pinLen, keyValue=$keyValue}")
        }

        override fun onConfirm(pinBlock: ByteArray?, isNonePin: Boolean) {
            // This will only be called in the case of PinBlockMKSK
            // pinBlock = f(PIN, PAN, Padding) with encryption using PIN_KEY at keySlot_99
            if (pinBlock == null || isNonePin) {
                runOnUiThread {
                    DebugUtil.logAndToast(
                        this@PinpadActivity,
                        TAG,
                        "Input is Empty!"
                    )
                }
                return
            }
            runOnUiThread {
                val result = buildString {
                    append("onConfirm - PinBlock encrypted using PIN_KEY at keySlot_99: \n")
                    append(String(pinBlock))
                    append("\n\nFor verification only:(decrypt+extract)\n")
                    append("PIN: ${PinpadUtil.getPinData(INDEX_NINETY_NINE, PAN, BytesUtil.hexString2Bytes(String(pinBlock)), TAG)}")
                    // Decrypt PIN from PINBlock must need PAN because PINBlock is generated using PAN
                }
                tvResult.text = result
            }
        }

        override fun onConfirm_dukpt(pinBlock: ByteArray?, ksn: ByteArray?) {
            // This will only be called in the case of PinBlockDukpt
            // Will be called whenever "Confirm" button is pressed
            // pinBlock = f(PIN, PAN, Padding) with encryption using Dukpt_PIN at keySlot_0
            // Each time KSN will increment
            if (pinBlock == null || ksn == null) {
                runOnUiThread {
                    DebugUtil.logAndToast(
                        this@PinpadActivity,
                        TAG,
                        "Input is Empty!"
                    )
                }
                return
            }
            runOnUiThread {
                tvResult.text = buildString {
                    append("PINBlock: ${BytesUtil.bytes2HexString(pinBlock)}\n")
                    append("ksn: \n${BytesUtil.bytes2HexString(ksn)}")
                }
            }
        }

        override fun onCancel() {
            // Will be called whenever "Cancel" button is pressed
            runOnUiThread {
                DebugUtil.logAndToast(this@PinpadActivity, TAG, "PinPad cancelled!")
            }
        }

        override fun onTimeOut() {
            // Will be called when Pin Pad timeout
            runOnUiThread {
                DebugUtil.logAndToast(this@PinpadActivity, TAG, "PinPad timeout!")
            }
        }

        override fun onError(errorCode: Int) {
            runOnUiThread {
                DebugUtil.logAndToast(this@PinpadActivity, TAG, "onError: errorCode=$errorCode")
                // e.g., return -1 if no Pin_Dukpt_Index specified; return 23 if PIN_KEY not exists
            }
        }
    }

    // <---------------------Helper methods---------------------> //

    private fun getJson(fileName: String, context: Context): String {
        runCatching {
           return context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.onFailure {
            it.printStackTrace()
        }
        return ""
    }

    private fun getImageFromAssetsFile(context: Context, fileName: String): Bitmap? {
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

    /*
        private Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
     */

}

enum class Dukpt(val index: Int) {
    MSR(1),
    EMV(2),
    PIN(3),
    MAC(4)
}

enum class PinParams(val value: String) {
    CARD_NO("cardNo"),
    TITLE("title"),
    MESSAGE("message"),
    SOUND("sound"),
    BYPASS("bypass"),
    SUPPORT_PIN_LEN("supportPinLen"),
    FULL_SCREEN("FullScreen"),
    ONLINE_PIN("onlinePin"),
    TIMEOUT_MS("timeOutMS"),
    PIN_KEY_NO("PINKeyNo"),
    RANDOM_KEYBOARD("randomKeyboard"),
    INPUT_TYPE("inputType"),
}