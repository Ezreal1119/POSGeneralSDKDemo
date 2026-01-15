package com.example.posgeneralsdkdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.posgeneralsdkdemo.utils.PermissionUtil.ensureAllFilesAccess

import com.urovo.i9000s.api.emv.ContantPara
import com.urovo.i9000s.api.emv.EmvListener
import com.urovo.i9000s.api.emv.EmvNfcKernelApi
import com.urovo.i9000s.api.emv.Funs
import com.urovo.sdk.pinpad.PinPadProviderImpl
import com.urovo.sdk.pinpad.listener.PinInputListener
import com.urovo.sdk.pinpad.utils.Constant
import com.urovo.sdk.utils.BytesUtil
import java.util.Hashtable

// <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>

private const val TAG = "EmvActivity"
class EmvActivity : AppCompatActivity() {

    private val tvPinDukptReady by lazy { findViewById<TextView>(R.id.tvPinDukptReady) }
    private val tvPinKeyReady by lazy { findViewById<TextView>(R.id.tvPinKeyReady) }
    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val etAmount by lazy { findViewById<EditText>(R.id.etAmount) }
    private val btnInitAidCapk by lazy { findViewById<Button>(R.id.btnInitAidCapk) }
    private val btnStartEmv by lazy { findViewById<Button>(R.id.btnStartEmv) }
    private val btnStopEmv by lazy { findViewById<Button>(R.id.btnStopEmv) }
    private val btnEnableLogOut by lazy { findViewById<Button>(R.id.btnEnableLogOut) }
    private val btnDisableLogOut by lazy { findViewById<Button>(R.id.btnDisableLogOut) }
    private val btnExportEmvLog by lazy { findViewById<Button>(R.id.btnExportEmvLog) }
    private val btnDeleteEmvLog by lazy { findViewById<Button>(R.id.btnDeleteEmvLog) }

    // If "context = null", then the log file will be stored to "/sdcard/UROPE"
    // In this case, the log file will be store to "/data/data/com.example.posgeneralsdkdemo/files/UROPE"
    private val mEmvKernelManager by lazy { EmvNfcKernelApi.getInstance(this) }
    private val mPinpadManager = PinPadProviderImpl.getInstance()

    private val result = StringBuilder()
    private var enterAmountAfterReadRecordFlag: Boolean = false
    private lateinit var cardReadMode: CardReadMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emv)

        btnInitAidCapk.setOnClickListener { onInitAidCapkButtonClicked() }
        btnStartEmv.setOnClickListener { onStartEmvButtonClicked(ContantPara.CheckCardMode.SWIPE_OR_INSERT_OR_TAP) }
        btnStopEmv.setOnClickListener { onStopEmvButtonClicked() }
        btnEnableLogOut.setOnClickListener { onEnableLogOutButtonClicked() }
        btnDisableLogOut.setOnClickListener { onDisableLogOutButtonClicked() }
        btnExportEmvLog.setOnClickListener { onExportEmvLogButtonClicked() }
        btnDeleteEmvLog.setOnClickListener { onDeleteEmvLogButtonClicked() }

        mEmvKernelManager.setListener(mEmvListener)

        runCatching {
            updateTerminalParameters()
        }.onSuccess {
            Toast.makeText(this, "Updated Terminal Params successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = buildString {
                append("Terminal Parameters updated:\n\n")
                append("IFD_SN_ASCII(9F1E): 12345678\n\n")
                append("TID_ASCII(9F1C): 00000000\n\n")
                append("Terminal_Type(9F35): Support Online Transaction(22)\n\n")
                append("Merchant_Category(9F15): Hotel(7011)\n\n")
                append("Transaction_Currency_Exp(5F36): 02\n\n")
                append("Currency(5F2A): EURO(0978)\n\n")
                append("Country(9F1A): Spain(0724)\n\n")
                append("Terminal_Capabilities(9F33): E0F8C8(Support All Cards; All CVM; All ODA)\n\n")
                append("Additional_Terminal_Capabilities(9F40): 6000F0A001")
            }
        }.onFailure {
            Toast.makeText(this, "Updated Terminal Params failed", Toast.LENGTH_SHORT).show()
            tvResult.text = it.message
            it.printStackTrace()
        }
        btnStartEmv.isEnabled = false
        mEmvKernelManager.updateAID(ContantPara.Operation.CLEAR, null)
        mEmvKernelManager.updateCAPK(ContantPara.Operation.CLEAR, null)

        mEmvKernelManager.LogOutEnable(0)
        btnEnableLogOut.isEnabled = true
        btnDisableLogOut.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        if (mPinpadManager.DukptGetKsn(3, ByteArray(10)) != 0x00) {
            tvPinDukptReady.text = "PinDukpt is not Ready!"
            tvPinDukptReady.setTextColor(Color.RED)
        } else {
            tvPinDukptReady.text = "PinDukpt is Ready"
            tvPinDukptReady.setTextColor(Color.GREEN)
        }
        if (mPinpadManager.isKeyExist(Constant.KeyType.PIN_KEY, 99)) {
            tvPinKeyReady.text = "MK/SK PinKey_99 is Ready"
            tvPinKeyReady.setTextColor(Color.GREEN)
        } else {
            tvPinKeyReady.text = "MK/SK PinKey_99 is not Ready!"
            tvPinKeyReady.setTextColor(Color.RED)
        }
//        if (mPinpadManager.DukptGetKsn(3, ByteArray(10)) != 0x00 && !mPinpadManager.isKeyExist(Constant.KeyType.PIN_KEY, 99)) {
//            btnStartEmv.isEnabled = false
//        }
        btnStopEmv.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        mEmvKernelManager.abortKernel()
    }

    private fun onInitAidCapkButtonClicked() {
        runCatching {
            addAidVisa()
            addCapkVisa()

            addAidMasterCard()
            addCapkMasterCard()

            addAidUpi()
            addCapkUpi()
        }.onSuccess {
            Toast.makeText(this, "Initializing AID&CAPK successfully", Toast.LENGTH_SHORT).show()
            btnInitAidCapk.isEnabled = false
            btnStartEmv.isEnabled = true
            tvResult.text = buildString {
                append("AIDs added: \n\n")
                append("A0000000031010 - Visa\n")
                append("A0000000041010 - MasterCard\n")
                append("A000000333010101 - UnionPay\n")
                append("A000000333010102 - UnionPay\n")
                append("A000000333010103 - UnionPay\n")
                append("A000000333010106 - UnionPay\n")
                append("A000000333010108 - UnionPay\n")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onStartEmvButtonClicked(cardMode: ContantPara.CheckCardMode) {
        val transParams by lazy {
            Hashtable<String, Any>().apply {
                put(TransactionTag.CHECK_CARD_MODE.tag, cardMode)
                put(TransactionTag.CURRENCY_CODE.tag, "978")
                put(
                    TransactionTag.EMV_OPTION.tag,
                    ContantPara.EmvOption.START
                ) // or START_WITH_FORCE_ONLINE
                put(TransactionTag.AMOUNT.tag, etAmount.text.toString()) // 1.00 by default
                put(TransactionTag.CASHBACK_AMOUNT.tag, "") // 0 cashback
                put(TransactionTag.CHECK_CARD_TIMEOUT.tag, "30") // 30 seconds
                put(TransactionTag.TRANSACTION_TYPE.tag, "00") // Purchase goods
                put(TransactionTag.FALLBACK_SWITCH.tag, "0") // FALLBACK is disabled
                put(TransactionTag.ENTER_AMOUNT_AFTER_READ_RECORD.tag, enterAmountAfterReadRecordFlag)
                put(TransactionTag.SUPPORT_DRL.tag, true) // Support Visa's Dynamic Reading Limit
                put(
                    TransactionTag.ENABLE_BEEPER.tag,
                    true
                ) // Enable the beeper when reading Card successfully
                put(
                    TransactionTag.ENABLE_TAP_SWIPE_COLLISION.tag,
                    false
                ) // Use the first payment method detected when encounter a collision within a short period of time.
                put(
                    TransactionTag.PRIORITIZED_CANDIDATE_APP.tag,
                    "A0000000031010"
                ) // Prioritize Visa Credit
                put(
                    TransactionTag.DISABLE_CHECK_MSR_FORMAT.tag,
                    true
                ) // Disable checking the validity of the format of the Magnetic Strip Card's Track Data's Format
            }
        }
        enterAmountAfterReadRecordFlag =
            etAmount.text.toString().toDoubleOrNull() == null || etAmount.text.toString().toDoubleOrNull() == 0.00
        Thread {
            runCatching {
                runOnUiThread {
                    Toast.makeText(this, "Please Insert/Tap/Swipe Card", Toast.LENGTH_SHORT).show()
                    btnStartEmv.isEnabled = false
                    btnStopEmv.isEnabled = true
                    tvResult.text = ""
                }
                result.clear()
                mEmvKernelManager.startKernel(transParams)
            }.onFailure {
                it.printStackTrace()
                runOnUiThread {
                    btnStartEmv.isEnabled = true
                    btnStopEmv.isEnabled = false
                }
            }
        }.start()
    }

    private fun onStopEmvButtonClicked() {
        runCatching {
            mEmvKernelManager.abortKernel()
        }.onSuccess {
            Toast.makeText(this, "Terminated", Toast.LENGTH_SHORT).show()
            btnStartEmv.isEnabled = true
            btnStopEmv.isEnabled = false
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onEnableLogOutButtonClicked() {
        runCatching {
            mEmvKernelManager.LogOutEnable(1)
        }.onSuccess {
            Toast.makeText(this, "Turn on EmvLogOut successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = buildString {
                append("The EMV log will be save to \n")
                append(" - \"/data/data/<package>/files/UROPE/\"\n\n")
                append("If want to export the file to \n")
                append(" - \"/sdcard/UROPE/\"\n\n")
                append("Please call: \n")
                append(" - EmvNfcKernelApi.exportLogFilesToExternalStorage(this)\n\n")
            }
            btnEnableLogOut.isEnabled = false
            btnDisableLogOut.isEnabled = true
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onDisableLogOutButtonClicked() {
        runCatching {
            mEmvKernelManager.LogOutEnable(0)
        }.onSuccess {
            Toast.makeText(this, "Turn off EmvLogOut successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = "Please note: \nEven tho Emv log are still output when it's disabled, those files have no valid Emv data and file sizes are very small."
            btnEnableLogOut.isEnabled = true
            btnDisableLogOut.isEnabled = false
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onExportEmvLogButtonClicked() {
        if (!ensureAllFilesAccess(this)) {
            Toast.makeText(this, "Please grant file permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val ret = EmvNfcKernelApi.exportLogFilesToExternalStorage(this)
            if (!ret) throw Exception("Export Emv Log failed")
        }.onSuccess {
            Toast.makeText(this, "Export EMV log to \"/sdcard/UROPE/\" successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = buildString {
                append("Please find the Emv Log in\n")
                append(" - \"/sdcard/UROPE/\"")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onDeleteEmvLogButtonClicked() {
        if (!ensureAllFilesAccess(this)) {
            Toast.makeText(this, "Please grant file permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val ret = EmvNfcKernelApi.deleteLogFiles(this)
            if (!ret) throw Exception("Delete Emv Log failed")
        }.onSuccess {
            Toast.makeText(this, "Delete EMV log successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = buildString {
                append("Both EMV log files in:\n")
                append(" - /sdcard/UROPE/\n")
                append(" - /data/data/<package>/files/UROPE/\n")
                append("were deleted")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    // <----------------Initialization Helper methods---------------->

    private fun updateTerminalParameters() {
        val terminalParameters = buildString {
            append(EmvTag.IFD_SN_ASCII_8.tag) // 9F1E
            append(EmvTag.IFD_SN_ASCII_8.len) // 08
            append("3132333435363738") // 12345678

            append(EmvTag.TERMINAL_IDENTIFICATION_ASCII_8.tag) // 9F1C
            append(EmvTag.TERMINAL_IDENTIFICATION_ASCII_8.len) // 08
            append("3030303030303030") // 00000000

            append(EmvTag.TERMINAL_TYPE.tag) // 9F35
            append(EmvTag.TERMINAL_TYPE.len) // 01
            append("22") // 0x22 means support Online transaction. 0x14 means not support Online.

            append(EmvTag.MERCHANT_CATEGORY_CODE.tag) // 9F15
            append(EmvTag.MERCHANT_CATEGORY_CODE.len) // 02
            append("7011") // 7011 means Hotel

            append(EmvTag.TRANSACTION_CURRENCY_EXPONENT.tag) // 5F36
            append(EmvTag.TRANSACTION_CURRENCY_EXPONENT.len) // 01
            append("02") // The Currency Exponent is only 02, e.g., $9.99

            append(EmvTag.CURRENCY_CODE.tag) // 5F2A
            append(EmvTag.CURRENCY_CODE.len) // 02
            append("0978") // The currency is EURO

            append(EmvTag.COUNTRY_CODE.tag) // 9F1A
            append(EmvTag.COUNTRY_CODE.len) // 02
            append("0724") // The country is Spain

            // Byte_1(Supported Card Type): 1110 0000 means:
            //  - Manual_Key_Entry(bit_8) supported
            //  - Mag_Strip_Card(bit_7) supported
            //  - ICC(bit_6) supported
            //  - Other bits are for RFU(Reserved for Use) which will be defined by the Kernel
            // Byte_2(Support CVM type): 1111 0000 means:
            //  - offlinePIN_plaintext(bit_8) supported
            //  - onlinePIN_Dukpt(bit_7) supported
            //  - Signature(bit_6) supported
            //  - offlinePIN_enciphered(bit_5) supported
            //  - No CVM(bit_4) supported
            //  - Other bits are for RFU(Reserved for Use) which will be defined by the Kernel
            // Byte_3(Supported Security methods): 1100 1000 means:
            //  - Support_SDA(bit_8)
            //  - Support_DDA(bit_7)
            //  - Support_CDA(bit_4)
            //  - Other bits are for RFU(Reserved for Use) which will be defined by the Kernel
            append(EmvTag.TERMINAL_CAPABILITIES.tag) // 9F33
            append(EmvTag.TERMINAL_CAPABILITIES.len) // 03
            append("E0F8C8")

            append(EmvTag.ADDITIONAL_TERMINAL_CAPABILITIES.tag) // 9F40
            append(EmvTag.ADDITIONAL_TERMINAL_CAPABILITIES.len) // 05
            append("6000F0A001") // Need to refer to the Kernel Document
        }
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.UNKNOWN, terminalParameters)
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.ICC, terminalParameters)
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.PICC, terminalParameters)
    }

    private fun addAidUpi() { // UnionPay International
        // 1. Contact (ICC)
        val iccAid = Hashtable<String, String>().apply {
            // UnionPay Debit ICC:
            //  - AID: A000000333010101
            //  - Floor_Limit: 000000
            put(AidTag.CARD_TYPE.tag, "IcCard")
            put(AidTag.AID.tag, "A000000333010101") // UnionPay Debit ICC
            put(AidTag.APP_VERSION.tag, "0030")
            put(AidTag.TERMINAL_FLOOR_LIMIT.tag, "00000000")  // Means must go online
            put(AidTag.CONTACT_TAC_DENIAL.tag, "0000000000") // Means won't trigger TAC_DENIAL at all
            put(AidTag.CONTACT_TAC_DEFAULT.tag, "D84000A800")
            put(AidTag.CONTACT_TAC_ONLINE.tag, "DC4004F800") // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AidTag.THRESHOLD_VALUE.tag, "000000000000") // // Threshold amount of money that triggers certain logic(e.g., Need to go online); No effect in this case
            put(AidTag.TARGET_PERCENTAGE.tag, "99") // Used by TRM to tell TAA to 99% force online (Randomly select)
            put(AidTag.MAX_TARGET_PERCENTAGE.tag, "99") // Used by TRM to tell TAA to Up_To 99% force online (Randomly select) - Dynamically adjusting
            put(AidTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must partially match the Application's ID
            put(AidTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is a 4 Bytes Random Number if Card doesn't specify
        }
        mEmvKernelManager.apply {
            updateAID(ContantPara.Operation.ADD, iccAid)
            iccAid.put("aid", "A000000333010102") // UnionPay Debit ICC
            updateAID(ContantPara.Operation.ADD, iccAid)
            iccAid.put("aid", "A000000333010103") // UnionPay Debit ICC
            updateAID(ContantPara.Operation.ADD, iccAid)
            iccAid.put("aid", "A000000333010106") // UnionPay Debit ICC
            updateAID(ContantPara.Operation.ADD, iccAid)
            iccAid.put("aid", "A000000333010108") // UnionPay Debit ICC
            updateAID(ContantPara.Operation.ADD, iccAid)
        }

        // 2. Contactless (PICC)
        val piccAid = Hashtable<String, String>().apply {
            put(AidTag.CARD_TYPE.tag, "UpiCard")
            put(AidTag.APPLICATION_IDENTIFIER.tag, "A000000333010101") // UnionPay International PICC
            put(AidTag.TERMINAL_TRANSACTION_QUALIFIERS.tag, "36004000")
            put(AidTag.TRANSACTION_LIMIT.tag, "000005000000") // Can't process Transaction Amount above 50000.00
            put(AidTag.FLOOR_LIMIT.tag, "000000000000") // Means always force online
            put(AidTag.CVM_REQUIRED_LIMIT.tag, "000000030000") // Means more than 300 must need CVM
            put(AidTag.LIMIT_SWITCH.tag, "FE00")
            put(AidTag.EMV_TERMINAL_FLOOR_LIMIT.tag, "00000000")
        }
        mEmvKernelManager.apply {
            updateAID(ContantPara.Operation.ADD, piccAid)
            iccAid.put("aid", "A000000333010102") // UnionPay International PICC
            updateAID(ContantPara.Operation.ADD, piccAid)
            iccAid.put("aid", "A000000333010103") // UnionPay International PICC
            updateAID(ContantPara.Operation.ADD, piccAid)
            iccAid.put("aid", "A000000333010106") // UnionPay International PICC
            updateAID(ContantPara.Operation.ADD, piccAid)
            iccAid.put("aid", "A000000333010108") // MUnionPay International PICC
            updateAID(ContantPara.Operation.ADD, piccAid)
        }
    }

    private fun addCapkUpi() { // UnionPay International
        mEmvKernelManager.apply {
            // "9F06" - RID(Registered Application Provider Identifier): A000000333 - UnionPay International
            // "9F22" - CAPK_Index(used by the Card to specify which UPI_CAPK to use): 08
            // "DF04" - CAPK_Exponent: 03(0000 0011)
            // "DF05" - CAPK_Expiry_Date: 20401231
            // "DF02" - CAPK_MODULUS: 144 Bytes (1152 bits)
            // "DF03" - CAPK_CheckSum: 00000000000000000000000000000000000000 (19 Bytes)
            updateCAPK_TLV(ContantPara.CardSlot.UNKNOWN,"9F0605A0000003339F220108DF040103DF050420401231DF028190B61645EDFD5498FB246444037A0FA18C0F101EBD8EFA54573CE6E6A7FBF63ED21D66340852B0211CF5EEF6A1CD989F66AF21A8EB19DBD8DBC3706D135363A0D683D046304F5A836BC1BC632821AFE7A2F75DA3C50AC74C545A754562204137169663CFCC0B06E67E2109EBA41BC67FF20CC8AC80D7B6EE1A95465B3B2657533EA56D92D539E5064360EA4850FED2D1BFDF031300000000000000000000000000000000000000")

            // "9F06" - RID(Registered Application Provider Identifier): A000000333 - UnionPay International
            // "9F22" - CAPK_Index(used by the Card to specify which UPI_CAPK to use): 09
            // "DF04" - CAPK_Exponent: 03(0000 0011)
            // "DF05" - CAPK_Expiry_Date: 20401231
            // "DF02" - CAPK_MODULUS: 176 Bytes (1408 bits)
            // "DF03" - CAPK_CheckSum: 00000000000000000000000000000000000000 (19 Bytes)
            updateCAPK_TLV(ContantPara.CardSlot.UNKNOWN, "9F0605A0000003339F220109DF040103DF050420401231DF0281B0EB374DFC5A96B71D2863875EDA2EAFB96B1B439D3ECE0B1826A2672EEEFA7990286776F8BD989A15141A75C384DFC14FEF9243AAB32707659BE9E4797A247C2F0B6D99372F384AF62FE23BC54BCDC57A9ACD1D5585C303F201EF4E8B806AFB809DB1A3DB1CD112AC884F164A67B99C7D6E5A8A6DF1D3CAE6D7ED3D5BE725B2DE4ADE23FA679BF4EB15A93D8A6E29C7FFA1A70DE2E54F593D908A3BF9EBBD760BBFDC8DB8B54497E6C5BE0E4A4DAC29E5DF031300000000000000000000000000000000000000")

            // "9F06" - RID(Registered Application Provider Identifier): A000000333 - UnionPay International
            // "9F22" - CAPK_Index(used by the Card to specify which UPI_CAPK to use): 0A
            // "DF04" - CAPK_Exponent: 03(0000 0011)
            // "DF05" - CAPK_Expiry_Date: 20401231
            // "DF02" - CAPK_MODULUS: 256 Bytes (2048 bits)
            // "DF03" - CAPK_CheckSum: 00000000000000000000000000000000000000 (19 Bytes)
            updateCAPK_TLV(ContantPara.CardSlot.UNKNOWN, "9F0605A0000003339F22010ADF040103DF050420401231DF028180B2AB1B6E9AC55A75ADFD5BBC34490E53C4C3381F34E60E7FAC21CC2B26DD34462B64A6FAE2495ED1DD383B8138BEA100FF9B7A111817E7B9869A9742B19E5C9DAC56F8B8827F11B05A08ECCF9E8D5E85B0F7CFA644EFF3E9B796688F38E006DEB21E101C01028903A06023AC5AAB8635F8E307A53AC742BDCE6A283F585F48EFDF031300000000000000000000000000000000000000")

            // "9F06" - RID(Registered Application Provider Identifier): A000000333 - UnionPay International
            // "9F22" - CAPK_Index(used by the Card to specify which UPI_CAPK to use): 0B
            // "DF04" - CAPK_Exponent: 03(0000 0011)
            // "DF05" - CAPK_Expiry_Date: 20401231
            // "DF02" - CAPK_MODULUS: 496 Bytes (3968 bits)
            // "DF03" - CAPK_CheckSum: 00000000000000000000000000000000000000 (19 Bytes)
            updateCAPK_TLV(ContantPara.CardSlot.UNKNOWN,"9F0605A0000003339F22010BDF040103DF050420401231DF0281F8CF9FDF46B356378E9AF311B0F981B21A1F22F250FB11F55C958709E3C7241918293483289EAE688A094C02C344E2999F315A72841F489E24B1BA0056CFAB3B479D0E826452375DCDBB67E97EC2AA66F4601D774FEAEF775ACCC621BFEB65FB0053FC5F392AA5E1D4C41A4DE9FFDFDF1327C4BB874F1F63A599EE3902FE95E729FD78D4234DC7E6CF1ABABAA3F6DB29B7F05D1D901D2E76A606A8CBFFFFECBD918FA2D278BDB43B0434F5D45134BE1C2781D157D501FF43E5F1C470967CD57CE53B64D82974C8275937C5D8502A1252A8A5D6088A259B694F98648D9AF2CB0EFD9D943C69F896D49FA39702162ACB5AF29B90BADE005BC157DF031300000000000000000000000000000000000000");
        }
    }

    private fun addAidVisa() {
        val aid = Hashtable<String, String>().apply {
            put(AidTag.CARD_TYPE.tag, "IcCard")
            put(AidTag.AID.tag, "A0000000031010") // Visa Credit
            put(AidTag.APP_VERSION.tag, "0002")
            put(AidTag.TERMINAL_FLOOR_LIMIT.tag, "00000000") // Means must go online
            put(AidTag.CONTACT_TAC_DENIAL.tag, "0000000000") // Means won't trigger TAC_DENIAL at all
            put(AidTag.CONTACT_TAC_ONLINE.tag, "DC4004F800") // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AidTag.CONTACT_TAC_DEFAULT.tag, "BC78BCA800")
            put(AidTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is 4 Bytes Random Number if Card doesn't specify
            put(AidTag.DEFAULT_TDOL.tag, "9F0206") // DEFAULT TDOL is 6 Bytes Amount if Card doesn't specify
            put(AidTag.ACQUIRER_IDENTIFIER.tag, "303030313131")
            put(AidTag.THRESHOLD_VALUE.tag, "000000002000") // Threshold amount of money that triggers certain logic(e.g., Need to go online). 20.00 in this case
            put(AidTag.TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
            put(AidTag.MAX_TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
            put(AidTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must fully match the Application's ID
            put(AidTag.TRANSACTION_CURRENCY_CODE.tag, "0978") // The currency used for this App is EURO
            put(AidTag.TRANSACTION_CURRENCY_CODE_EXPONENT.tag, "02") // The currency used for this App is 02
        }
        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, aid)
    }

    private fun addCapkVisa() {
        // 1. INDEX_08
        var capk = Hashtable<String, String>().apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "08") // VISA demands this CAPK to be store at the Index of 08 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0B")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 2. INDEX_09
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "09") // VISA demands this CAPK to be store at the Index of 09 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "9D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC4926D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC5521ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED56481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C026DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 3. INDEX_53
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "53") // VISA demands this CAPK to be store at the Index of 53 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "BCD83721BE52CCCC4B6457321F22A7DC769F54EB8025913BE804D9EABBFA19B3D7C5D3CA658D768CAF57067EEC83C7E6E9F81D0586703ED9DDDADD20675D63424980B10EB364E81EB37DB40ED100344C928886FF4CCC37203EE6106D5B59D1AC102E2CD2D7AC17F4D96C398E5FD993ECB4FFDF79B17547FF9FA2AA8EEFD6CBDA124CBB17A0F8528146387135E226B005A474B9062FF264D2FF8EFA36814AA2950065B1B04C0A1AE9B2F69D4A4AA979D6CE95FEE9485ED0A03AEE9BD953E81CFD1EF6E814DFD3C2CE37AEFA38C1F9877371E91D6A5EB59FDEDF75D3325FA3CA66CDFBA0E57146CC789818FF06BE5FCC50ABD362AE4B80996D")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 4. INDEX_57
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "57") // VISA demands this CAPK to be store at the Index of 57 of the POS
            put(CapkTag.EXPONENT.tag, "010001") // 0001 0001
            put(CapkTag.MODULUS.tag, "942B7F2BA5EA307312B63DF77C5243618ACC2002BD7ECB74D821FE7BDC78BF28F49F74190AD9B23B9713B140FFEC1FB429D93F56BDC7ADE4AC075D75532C1E590B21874C7952F29B8C0F0C1CE3AEEDC8DA25343123E71DCF86C6998E15F756E3")
            put(CapkTag.CHECKSUM.tag, "429C954A3859CEF91295F663C963E582ED6EB253")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 5. INDEX_92
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "92") // VISA demands this CAPK to be store at the Index of 92 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F")
            put(CapkTag.CHECKSUM.tag, "429C954A3859CEF91295F663C963E582ED6EB253")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 6. INDEX_94
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "94") // VISA demands this CAPK to be store at the Index of 94 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "ACD2B12302EE644F3F835ABD1FC7A6F62CCE48FFEC622AA8EF062BEF6FB8BA8BC68BBF6AB5870EED579BC3973E121303D34841A796D6DCBC41DBF9E52C4609795C0CCF7EE86FA1D5CB041071ED2C51D2202F63F1156C58A92D38BC60BDF424E1776E2BC9648078A03B36FB554375FC53D57C73F5160EA59F3AFC5398EC7B67758D65C9BFF7828B6B82D4BE124A416AB7301914311EA462C19F771F31B3B57336000DFF732D3B83DE07052D730354D297BEC72871DCCF0E193F171ABA27EE464C6A97690943D59BDABB2A27EB71CEEBDAFA1176046478FD62FEC452D5CA393296530AA3F41927ADFE434A2DF2AE3054F8840657A26E0FC617")
            put(CapkTag.CHECKSUM.tag, "C4A3C43CCF87327D136B804160E47D43B60E6E0F")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 7. INDEX_96
        capk.apply {
            put(CapkTag.RID.tag, "A000000003") // VISA
            put(CapkTag.INDEX.tag, "96") // VISA demands this CAPK to be store at the Index of 96 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "B74586D19A207BE6627C5B0AAFBC44A2ECF5A2942D3A26CE19C4FFAEEE920521868922E893E7838225A3947A2614796FB2C0628CE8C11E3825A56D3B1BBAEF783A5C6A81F36F8625395126FA983C5216D3166D48ACDE8A431212FF763A7F79D9EDB7FED76B485DE45BEB829A3D4730848A366D3324C3027032FF8D16A1E44D8D")
            put(CapkTag.CHECKSUM.tag, "C63D0D8598AA7A5AA342FB80489C39A2A6E5A5F7")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)
    }

    private fun addAidMasterCard() {
        val aid = Hashtable<String, String>().apply {
            put(AidTag.CARD_TYPE.tag, "IcCard")
            put(AidTag.AID.tag, "A0000000041010") // MasterCard Credit
            put(AidTag.APP_VERSION.tag, "0002")
            put(AidTag.TERMINAL_FLOOR_LIMIT.tag, "00000000") // Means must go online
            put(AidTag.CONTACT_TAC_DENIAL.tag, "0000000000") // Means won't trigger TAC_DENIAL at all
            put(AidTag.CONTACT_TAC_ONLINE.tag, "DC4004F800") // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AidTag.CONTACT_TAC_DEFAULT.tag, "BC78BCA800")
            put(AidTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is 4 Bytes Random Number if Card doesn't specify
            put(AidTag.DEFAULT_TDOL.tag, "9F0206") // DEFAULT TDOL is 6 Bytes Amount if Card doesn't specify
            put(AidTag.ACQUIRER_IDENTIFIER.tag, "303030313131")
            put(AidTag.THRESHOLD_VALUE.tag, "000000002000") // Threshold amount of money that triggers certain logic(e.g., Need to go online). 20.00 in this case
            put(AidTag.TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
            put(AidTag.MAX_TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
            put(AidTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must fully match the Application's ID
            put(AidTag.TRANSACTION_CURRENCY_CODE.tag, "0978") // The currency used for this App is EURO
            put(AidTag.TRANSACTION_CURRENCY_CODE_EXPONENT.tag, "02") // The currency used for this App is 02
        }
        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, aid)
    }

    private fun addCapkMasterCard() {
        // 1. INDEX_04
        var capk = Hashtable<String, String>().apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "04") // MasterCard demands this CAPK to be store at the Index of 04 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A6DA428387A502D7DDFB7A74D3F412BE762627197B25435B7A81716A700157DDD06F7CC99D6CA28C2470527E2C03616B9C59217357C2674F583B3BA5C7DCF2838692D023E3562420B4615C439CA97C44DC9A249CFCE7B3BFB22F68228C3AF13329AA4A613CF8DD853502373D62E49AB256D2BC17120E54AEDCED6D96A4287ACC5C04677D4A5A320DB8BEE2F775E5FEC5")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 2. INDEX_05
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "05") // MasterCard demands this CAPK to be store at the Index of 05 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7BACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D4575916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938C68D095AAC91B5F37E28BB49EC7ED597")
            put(CapkTag.CHECKSUM.tag, "EBFA0D5D06D8CE702DA3EAE890701D45E274C845")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 3. INDEX_06
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "06") // MasterCard demands this CAPK to be store at the Index of 06 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F")
            put(CapkTag.CHECKSUM.tag, "F910A1504D5FFB793D94F3B500765E1ABCAD72D9")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 4. INDEX_EF
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "EF") // MasterCard demands this CAPK to be store at the Index of EF of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A191CB87473F29349B5D60A88B3EAEE0973AA6F1A082F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A13ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB651AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3B")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 5. INDEX_F1
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "F1") // MasterCard demands this CAPK to be store at the Index of F1 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7")
            put(CapkTag.CHECKSUM.tag, "D8E68DA167AB5A85D8C3D55ECB9B0517A1A5B4BB")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 6. INDEX_F3
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "F3") // MasterCard demands this CAPK to be store at the Index of F3 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA0124723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A350C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 7. INDEX_F8
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "F8") // MasterCard demands this CAPK to be store at the Index of F8 of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA84F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD190B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF6323FC8E9273D6F849198C4996209166D9BFC973C361CC826E1")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 8. INDEX_FA
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "FA") // MasterCard demands this CAPK to be store at the Index of FA of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A90FCD55AA2D5D9963E35ED0F440177699832F49C6BAB15CDAE5794BE93F934D4462D5D12762E48C38BA83D8445DEAA74195A301A102B2F114EADA0D180EE5E7A5C73E0C4E11F67A43DDAB5D55683B1474CC0627F44B8D3088A492FFAADAD4F42422D0E7013536C3C49AD3D0FAE96459B0F6B1B6056538A3D6D44640F94467B108867DEC40FAAECD740C00E2B7A8852D")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)

        // 9. INDEX_FE
        capk.apply {
            put(CapkTag.RID.tag, "A000000004") // MasterCard
            put(CapkTag.INDEX.tag, "FE") // MasterCard demands this CAPK to be store at the Index of FE of the POS
            put(CapkTag.EXPONENT.tag, "03") // 0000 0011
            put(CapkTag.MODULUS.tag, "A653EAC1C0F786C8724F737F172997D63D1C3251C44402049B865BAE877D0F398CBFBE8A6035E24AFA086BEFDE9351E54B95708EE672F0968BCD50DCE40F783322B2ABA04EF137EF18ABF03C7DBC5813AEAEF3AA7797BA15DF7D5BA1CBAF7FD520B5A482D8D3FEE105077871113E23A49AF3926554A70FE10ED728CF793B62A1")
            put(CapkTag.CHECKSUM.tag, "00000000000000000000000000000000000000")
        }
        mEmvKernelManager.updateCAPK(ContantPara.Operation.ADD, capk)
    }

    private val mEmvListener = object: EmvListener {
        override fun onRequestSetAmount() {
            // Call when ENTER_AMOUNT_AFTER_READ_RECORD=true and not entered any amount before kernel starts
            Log.e(TAG, "onRequestSetAmount: amount=1.00")
            if (enterAmountAfterReadRecordFlag) {
                mEmvKernelManager.setAmountEx("1.00", null)
            }
        }

        override fun onReturnCheckCardResult(
            checkCardResult: ContantPara.CheckCardResult,
            hashtable: Hashtable<String?, String?>
        ) {
            Log.e(TAG, "onReturnCheckCardResult: checkCardResult=$checkCardResult")
            Log.e(TAG, "onReturnCheckCardResult: hashtable$hashtable")
            when (checkCardResult) {
                ContantPara.CheckCardResult.MSR -> {
                    Log.e(TAG, "onReturnCheckCardResult: StripInfo=${hashtable.get("StripInfo")}")
                    Log.e(TAG, "onReturnCheckCardResult: CardNo=${hashtable.get("CardNo")}")
                    cardReadMode = CardReadMode.SWIPE
                    runOnUiThread { Toast.makeText(this@EmvActivity, "Magnetic Stripe Card detected", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.INSERTED_CARD -> {
                    cardReadMode = CardReadMode.CONTACT
                    runOnUiThread { Toast.makeText(this@EmvActivity, "ICC Card detected", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.TAP_CARD_DETECTED -> {
                    cardReadMode = CardReadMode.CONTACTLESS
                    runOnUiThread { Toast.makeText(this@EmvActivity, "PICC Card detected", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.NEED_FALLBACK -> {
                    Log.e(TAG, "onReturnCheckCardResult: NEED_FALLBACK")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "NEED_FALLBACK: Please Swipe or Tap!", Toast.LENGTH_SHORT).show() }
                    while (!mEmvKernelManager.CheckCardIsOut(10000)) { Thread.sleep(50) }
                    Log.e(TAG, "onReturnCheckCardResult: Card is out")
                    runOnUiThread {
                        onStartEmvButtonClicked(ContantPara.CheckCardMode.SWIPE_OR_TAP)
                    }
                }
                ContantPara.CheckCardResult.BAD_SWIPE -> {
                    Log.e(TAG, "onReturnCheckCardResult: BAD_SWIPE")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "BAD_SWIPE", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.NOT_ICC -> {
                    Log.e(TAG, "onReturnCheckCardResult: ")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "NOT_ICC", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.TIMEOUT -> {
                    Log.e(TAG, "onReturnCheckCardResult: TIMEOUT")
                    runOnUiThread {
                        Toast.makeText(this@EmvActivity, "TIMEOUT after 30s", Toast.LENGTH_SHORT).show()
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                    }
                }
                ContantPara.CheckCardResult.CANCEL -> {
                    Log.e(TAG, "onReturnCheckCardResult: CANCEL")
                }
                ContantPara.CheckCardResult.DEVICE_BUSY -> {
                    Log.e(TAG, "onReturnCheckCardResult: DEVICE_BUSY")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "DEVICE_BUSY", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.USE_ICC_CARD -> {
                    Log.e(TAG, "onReturnCheckCardResult: USE_ICC_CARD")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "USE_ICC_CARD", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.MULT_CARD -> {
                    Log.e(TAG, "onReturnCheckCardResult: MULT_CARD")
                    runOnUiThread { Toast.makeText(this@EmvActivity, "MULT_CARD", Toast.LENGTH_SHORT).show() }
                }
                else -> {
                    TODO("Not yet implemented")
                }
            }
        }

        override fun onRequestSelectApplication(appList: ArrayList<String?>) {
            // SELECT AID: if ICC has more than one Application, will callback this method
            Log.e(TAG, "onRequestSelectApplication: ")
            for (app in appList) Log.e(TAG, "onRequestSelectApplication: Get App=$app")
            TODO("Not yet implemented") // mKernelApi.selectApplication(index)
        }

        override fun onRequestPinEntry(pinEntrySource: ContantPara.PinEntrySource?) {
            // CVM - OnlinePIN: will callback this method
            Log.e(TAG, "onRequestPinEntry: CVM - OnlinePIN")
            result.append("CVM_OnlinePIN: ")
            val pinpadBundle = Bundle().apply {
                putBoolean(PinParams.ONLINE_PIN.value, true) // This is for Online PIN
                putString(PinParams.CARD_NO.value, getCardNo()) // The field is a Must for generating PINBlock
                putString(PinParams.TITLE.value, "PinPad - Emv Demo") // "" by default
                putString(PinParams.MESSAGE.value, "Please enter PIN, $${etAmount.text}") // "" by default
                putBoolean(PinParams.SOUND.value, false) // Sound will be turned on when using the PinPad (lasting effect); "false" by default
                putString(PinParams.SUPPORT_PIN_LEN.value, "6") // Will use the one set by last time by default. Thus, must set before using.
                putBoolean(PinParams.FULL_SCREEN.value, true) // true by default. Won't have Cancel button when half screen
                putLong(PinParams.TIMEOUT_MS.value, 30000) // Time out since opening the Pad. 0 by default, must set!
                putBoolean(PinParams.RANDOM_KEYBOARD.value, false) // true by default.
            }
            if (pinEntrySource == ContantPara.PinEntrySource.KEYPAD) {

                if (mPinpadManager.DukptGetKsn(Dukpt.PIN.index, ByteArray(10)) == 0x00) {
                    Log.e(TAG, "onRequestPinEntry: DUPKT Pinpad was called")
                    result.append("DUPKT Pinpad was called\n\n")
                    pinpadBundle.putInt(PinParams.PIN_KEY_NO.value, Dukpt.PIN.index) // Must set, will call onError otherwise
                    mPinpadManager.GetDukptPinBlock(pinpadBundle, mPinInputListener)

                } else {
                    Log.e(TAG, "onRequestPinEntry: MK/SK Pinpad was called")
                    result.append("MK/SK Pinpad was called\n\n")
                    pinpadBundle.putInt(PinParams.PIN_KEY_NO.value, 99) // The keySlot of the PIN_KEY to use for encryption. Must Set since onlinePin must be encrypted!
                    mPinpadManager.getPinBlockEx(pinpadBundle, mPinInputListener)
                }
            } else {
               TODO("Not yet implemented") // PHONE
            }
        }

        override fun onRequestOfflinePinEntry(
            pinEntrySource: ContantPara.PinEntrySource?,
            pinTryCount: Int
        ) {
            // Usually not use. Refer to onRequestOfflinePINVerify() for offline PIN
            Log.e(TAG, "onRequestOfflinePinEntry: ")
            TODO("Not yet implemented")
        }

        override fun onRequestConfirmCardno() {
            // After GPO, before PR
            Log.e(TAG, "onRequestConfirmCardno: After GPO, before ODA")
            logTVR(TAG, mEmvKernelManager.getValByTag(0x95))
            Log.e(TAG, "onRequestConfirmCardno: PAN=${mEmvKernelManager.getValByTag(0x5A)}") // PAN
            Log.e(TAG, "onRequestConfirmCardno: AID=${mEmvKernelManager.getValByTag(0x4F)} - ${hexToAscii(mEmvKernelManager.getValByTag(0x50))} - ${mEmvKernelManager.getValByTag(0x87)}") // AID - AID_ASCII - Priority
            Log.e(TAG, "onRequestConfirmCardno: Application Expiration Date=${mEmvKernelManager.getValByTag(0x5F24)}")
            Log.e(TAG, "onRequestConfirmCardno: Application Effective Date=${mEmvKernelManager.getValByTag(0x5F25)}")
            Log.e(TAG, "onRequestConfirmCardno: Application Issuer Country Code=${mEmvKernelManager.getValByTag(0x5F28)}")
            Log.e(TAG, "onRequestConfirmCardno: Transaction Amount=${mEmvKernelManager.getValByTag(0x9F02)}")
            Log.e(TAG, "onRequestConfirmCardno: Transaction Date=${mEmvKernelManager.getValByTag(0x9A)}")
            Log.e(TAG, "onRequestConfirmCardno: Transaction Type=${mEmvKernelManager.getValByTag(0x9C)} (\"00\"(Purchase); \"01\"(Withdrawal); \"09\"(CashBack); \"20\"(Refund))")
            Log.e(TAG, "onRequestConfirmCardno: Floor Limit=${mEmvKernelManager.getValByTag(0x9F1B)}")
            result.apply {
                append("PAN: ${getCardNo()}\n\n") // 5A
                append("AID: ${mEmvKernelManager.getValByTag(0x4F)} - ${hexToAscii(mEmvKernelManager.getValByTag(0x50))} - ${mEmvKernelManager.getValByTag(0x87)}\n\n")
                append("Application Expiration Date: ${mEmvKernelManager.getValByTag(0x5F24)}\n\n")
                append("Application Effective Date: ${mEmvKernelManager.getValByTag(0x5F25)}\n\n")
                append("Application Issuer Country Code: ${mEmvKernelManager.getValByTag(0x5F28)}\n\n")
                append("Transaction Amount: ${mEmvKernelManager.getValByTag(0x9F02)}\n\n")
                append("Transaction Date: ${mEmvKernelManager.getValByTag(0x9A)}\n\n")
                append("Transaction Type: ${mEmvKernelManager.getValByTag(0x9C)}\n\n")
                append("Floor Limit: ${mEmvKernelManager.getValByTag(0x9F1B)}\n\n")
            }
            // Confirm if cardNo is not null and not blank
            mEmvKernelManager.sendConfirmCardnoResult(mEmvKernelManager.getValByTag(0x5A).isNotBlank())
        }

        override fun onRequestFinalConfirm() {
            // Right before TAA
            Log.e(TAG, "onRequestFinalConfirm: Right before TAA (Already finished TVR)")
            result.apply {
                append("TVR: ")
                append(mEmvKernelManager.getValByTag(0x95))
            }
            logTVR(TAG, mEmvKernelManager.getValByTag(0x95))
            mEmvKernelManager.sendFinalConfirmResult(true)
//            Log.e(TAG, "==================== TVR(95) Bits Definition ====================")
//
//            Log.e(TAG, "[Byte1] Offline data authentication was not performed")
//            Log.e(TAG, "  - bit8 (0x80): Offline data authentication was not performed")
//            Log.e(TAG, "  - bit7 (0x40): SDA failed")
//            Log.e(TAG, "  - bit6 (0x20): ICC data missing")
//            Log.e(TAG, "  - bit5 (0x10): Card appears on terminal exception file")
//            Log.e(TAG, "  - bit4 (0x08): DDA failed")
//            Log.e(TAG, "  - bit3 (0x04): CDA failed")
//            Log.e(TAG, "  - bit2 (0x02): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit1 (0x01): Reserved for use by the schemes")
//
//            Log.e(TAG, "[Byte2] Application version / expired / service restrictions")
//            Log.e(TAG, "  - bit8 (0x80): ICC and terminal have different application versions")
//            Log.e(TAG, "  - bit7 (0x40): Expired application")
//            Log.e(TAG, "  - bit6 (0x20): Application not yet effective")
//            Log.e(TAG, "  - bit5 (0x10): Requested service not allowed for card product")
//            Log.e(TAG, "  - bit4 (0x08): New card")
//            Log.e(TAG, "  - bit3 (0x04): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit2 (0x02): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit1 (0x01): Reserved for use by the schemes")
//
//            Log.e(TAG, "[Byte3] Cardholder verification / risk management")
//            Log.e(TAG, "  - bit8 (0x80): Cardholder verification was not successful")
//            Log.e(TAG, "  - bit7 (0x40): Unrecognised CVM")
//            Log.e(TAG, "  - bit6 (0x20): PIN Try Limit exceeded")
//            Log.e(TAG, "  - bit5 (0x10): PIN entry required and PIN pad not present or not working")
//            Log.e(TAG, "  - bit4 (0x08): PIN entry required, PIN pad present, but PIN was not entered")
//            Log.e(TAG, "  - bit3 (0x04): Transaction exceeds floor limit")
//            Log.e(TAG, "  - bit2 (0x02): Lower consecutive offline limit exceeded")
//            Log.e(TAG, "  - bit1 (0x01): Upper consecutive offline limit exceeded")
//
//            Log.e(TAG, "[Byte4] Terminal / transaction processing")
//            Log.e(TAG, "  - bit8 (0x80): Transaction selected randomly for online processing")
//            Log.e(TAG, "  - bit7 (0x40): Merchant forced transaction online")
//            Log.e(TAG, "  - bit6 (0x20): Transaction was not performed with terminal capabilities")
//            Log.e(TAG, "  - bit5 (0x10): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit4 (0x08): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit3 (0x04): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit2 (0x02): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit1 (0x01): Reserved for use by the schemes")
//
//            Log.e(TAG, "[Byte5] Script processing")
//            Log.e(TAG, "  - bit8 (0x80): Default TDOL used")
//            Log.e(TAG, "  - bit7 (0x40): Issuer authentication failed")
//            Log.e(TAG, "  - bit6 (0x20): Script processing failed before final GENERATE AC")
//            Log.e(TAG, "  - bit5 (0x10): Script processing failed after final GENERATE AC")
//            Log.e(TAG, "  - bit4 (0x08): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit3 (0x04): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit2 (0x02): Reserved for use by the schemes")
//            Log.e(TAG, "  - bit1 (0x01): Reserved for use by the schemes")
//
//            Log.e(TAG, "=================================================================")
        }

        override fun onRequestOnlineProcess(cardTlvData: String?, dataKsn: String?) {
            // Form ISO8583(DE55_PINBlock_MAC) and send to Issuer, then forward the ARPC in GAC-2 to ICC
            // This is a simulation of the Issuer returning the Approval Data
            Log.e(TAG, "onRequestOnlineProcess: Sending ISO8583 and get ARPC from Issuer")
            val authorizationResponseCode = "8A023030" // ARC ["00"(3030): Approved; "05": Declined; "51": Insufficient balance]
            val authorizationCode = "8906${Funs.convertStringToHex("000001")}" // Approved Serial Number: "89" is the Prefix
            mEmvKernelManager.sendOnlineProcessResult(true, authorizationResponseCode + authorizationCode)
        }

        override fun onReturnBatchData(cardTlvData: String?) {
            Log.e(TAG, "onReturnBatchData: ")
            TODO("Not yet implemented")
        }

        override fun onReturnTransactionResult(transactionResult: ContantPara.TransactionResult?) {
            runOnUiThread {
                btnStartEmv.isEnabled = true
                btnStopEmv.isEnabled = false
                result.insert(0, "<=======$transactionResult=======>\n\n")
                tvResult.text = result
                Toast.makeText(this@EmvActivity, transactionResult.toString(), Toast.LENGTH_SHORT).show()
            }
            Log.e(TAG, "onReturnTransactionResult: $transactionResult")
        }

        override fun onRequestDisplayText(displayText: ContantPara.DisplayText?) {
            Log.e(TAG, "onRequestDisplayText: ")
            TODO("Not yet implemented")
        }

        override fun onRequestOfflinePINVerify(
            pinEntrySource: ContantPara.PinEntrySource?,
            pinEntryType: Int,
            bundle: Bundle?
        ) {
            // CVM - offlinePIN: will callback this method
            Log.e(TAG, "onRequestOfflinePINVerify: ")
            TODO("Not yet implemented")
        }

        override fun onReturnIssuerScriptResult(
            issuerScriptResult: ContantPara.IssuerScriptResult?,
            s: String?
        ) {
            Log.e(TAG, "onReturnIssuerScriptResult: ")
            TODO("Not yet implemented")
        }

        override fun onNFCrequestTipsConfirm(
            msgID: ContantPara.NfcTipMessageID?,
            msg: String?
        ) {
            Log.e(TAG, "onNFCrequestTipsConfirm: ")
            TODO("Not yet implemented")
        }

        override fun onReturnNfcCardData(hashtable: Hashtable<String?, String?>?) {
            Log.e(TAG, "onReturnNfcCardData: ")
            TODO("Not yet implemented")
        }

        override fun onNFCrequestOnline() {
            Log.e(TAG, "onNFCrequestOnline: ")
            TODO("Not yet implemented")
        }

        override fun onNFCrequestImportPin(type: Int, lastTimeFlag: Int, amt: String?) {
            Log.e(TAG, "onNFCrequestImportPin: ")
            TODO("Not yet implemented")
        }

        override fun onNFCTransResult(result: ContantPara.NfcTransResult?) {
            Log.e(TAG, "onNFCTransResult: ")
            TODO("Not yet implemented")
        }

        override fun onNFCErrorInfor(
            errorCode: ContantPara.NfcErrMessageID?,
            strErrInfo: String?
        ) {
            Log.e(TAG, "onNFCErrorInfor: ")
            TODO("Not yet implemented")
        }

        override fun onNFCrequestFinalSelect(aid: ByteArray?) {
            Log.e(TAG, "onNFCrequestFinalSelect: ")
            TODO("Not yet implemented")
        }
    }

    private val mPinInputListener = object: PinInputListener {

        override fun onInput(pinLen: Int, keyValue: Int) {
            // This will be called whenever a number button is pressed
            // KeyValue will not be exposed, so all keyValues are "2"
        }

        override fun onConfirm(pinBlock: ByteArray, isNonePin: Boolean) {
            // This will only be called in the case of PinBlockMKSK
            // pinBlock = f(PIN, PAN, Padding) with encryption using PIN_KEY at keySlot_99
            if (cardReadMode == CardReadMode.CONTACT || cardReadMode == CardReadMode.CONTACTLESS) {
                if (isNonePin) {
                    mEmvKernelManager.bypassPinEntry()
                } else {
                    Log.e(TAG, "onConfirm: pinBlock=${String(pinBlock)}")
                    mEmvKernelManager.sendPinEntry()
                }
            }
        }

        override fun onConfirm_dukpt(pinBlock: ByteArray?, ksn: ByteArray?) {
            // This will only be called in the case of PinBlockDukpt
            // Will be called whenever "Confirm" button is pressed
            // pinBlock = f(PIN, PAN, Padding) with encryption using Dukpt_PIN at keySlot_0
            // Each time KSN will increment
            if (cardReadMode == CardReadMode.CONTACT || cardReadMode == CardReadMode.CONTACTLESS) {
                if (pinBlock == null) {
                    mEmvKernelManager.bypassPinEntry()
                } else {
                    Log.e(TAG, "onConfirm_dukpt: pinBlock=${String(pinBlock)}")
                    Log.e(TAG, "onConfirm_dukpt: KSN=${BytesUtil.bytes2HexString(ksn)}")
                    mEmvKernelManager.sendPinEntry()
                }
            }
        }

        override fun onCancel() {
            // Will be called whenever "Cancel" button is pressed
            Log.e(TAG, "onCancel: ")
            mEmvKernelManager.cancelPinEntry()
        }

        override fun onTimeOut() {
            // Will be called when Pin Pad timeout
            Log.e(TAG, "onTimeOut: ")
            mEmvKernelManager.cancelPinEntry()
        }

        override fun onError(errorCode: Int) {
            Log.e(TAG, "onError: ")
            if (errorCode == 23) {
                runOnUiThread { Toast.makeText(this@EmvActivity, "PIN_KEY not exists", Toast.LENGTH_SHORT).show() }
            }
            mEmvKernelManager.cancelPinEntry()
        }
    }

    private fun getCardNo(): String {
        var cardNo = mEmvKernelManager.getValByTag(0x5A) // PAN
        if (cardNo == null || cardNo.isBlank()) {
            cardNo = mEmvKernelManager.getValByTag(0x57) // Track 2
            if (cardNo == null || cardNo.isBlank()) return ""
            cardNo = cardNo.substring(0, cardNo.uppercase().indexOf("D")) // might also be "=" ?
        }
        val lastChar = cardNo[cardNo.length - 1]
        if (lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
            cardNo = cardNo.substring(0, cardNo.length - 1)
        }
        return cardNo
    }

    fun hexToAscii(hex: String): String {
        val clean = hex.replace(" ", "").replace("\n", "")
        require(clean.length % 2 == 0) { "Hex length must be even" }

        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            bytes[i] = clean.substring(index, index + 2).toInt(16).toByte()
        }
        return String(bytes, Charsets.US_ASCII)
    }

    fun logTVR(tag: String, tvrHex: String?) {
        if (tvrHex == null) {
            Log.e(tag, "TVR is null")
            return
        }

        var hex = tvrHex.replace(" ", "").uppercase()

        if (hex.length != 10) {
            Log.e(tag, "Invalid TVR length, must be 5 bytes (10 hex chars). TVR=$hex")
            return
        }

        val tvr = hexToBytes(hex)

        Log.e(tag, "==================== TVR(95) Parse ====================")
        Log.e(tag, "TVR HEX = $hex")
        Log.e(
            tag,
            String.format(
                "Byte1=%02X Byte2=%02X Byte3=%02X Byte4=%02X Byte5=%02X",
                tvr[0].toInt() and 0xFF,
                tvr[1].toInt() and 0xFF,
                tvr[2].toInt() and 0xFF,
                tvr[3].toInt() and 0xFF,
                tvr[4].toInt() and 0xFF
            )
        )
        Log.e(tag, "--------------------------------------------------------")

        // Byte 1
        logBit(tag, tvr[0], 0x80, "[Byte1] bit8", "Offline data authentication was not performed")
        logBit(tag, tvr[0], 0x40, "[Byte1] bit7", "SDA failed")
        logBit(tag, tvr[0], 0x20, "[Byte1] bit6", "ICC data missing")
        logBit(tag, tvr[0], 0x10, "[Byte1] bit5", "Card appears on terminal exception file")
        logBit(tag, tvr[0], 0x08, "[Byte1] bit4", "DDA failed")
        logBit(tag, tvr[0], 0x04, "[Byte1] bit3", "CDA failed")
        logBit(tag, tvr[0], 0x02, "[Byte1] bit2", "Reserved for use by the schemes")
        logBit(tag, tvr[0], 0x01, "[Byte1] bit1", "Reserved for use by the schemes")

        // Byte 2
        logBit(tag, tvr[1], 0x80, "[Byte2] bit8", "ICC and terminal have different application versions")
        logBit(tag, tvr[1], 0x40, "[Byte2] bit7", "Expired application")
        logBit(tag, tvr[1], 0x20, "[Byte2] bit6", "Application not yet effective")
        logBit(tag, tvr[1], 0x10, "[Byte2] bit5", "Requested service not allowed for card product")
        logBit(tag, tvr[1], 0x08, "[Byte2] bit4", "New card")
        logBit(tag, tvr[1], 0x04, "[Byte2] bit3", "Reserved for use by the schemes")
        logBit(tag, tvr[1], 0x02, "[Byte2] bit2", "Reserved for use by the schemes")
        logBit(tag, tvr[1], 0x01, "[Byte2] bit1", "Reserved for use by the schemes")

        // Byte 3
        logBit(tag, tvr[2], 0x80, "[Byte3] bit8", "Cardholder verification was not successful")
        logBit(tag, tvr[2], 0x40, "[Byte3] bit7", "Unrecognised CVM")
        logBit(tag, tvr[2], 0x20, "[Byte3] bit6", "PIN Try Limit exceeded")
        logBit(tag, tvr[2], 0x10, "[Byte3] bit5", "PIN entry required and PIN pad not present or not working")
        logBit(tag, tvr[2], 0x08, "[Byte3] bit4", "PIN entry required, PIN pad present, but PIN was not entered")
        logBit(tag, tvr[2], 0x04, "[Byte3] bit3", "Transaction exceeds floor limit")
        logBit(tag, tvr[2], 0x02, "[Byte3] bit2", "Lower consecutive offline limit exceeded")
        logBit(tag, tvr[2], 0x01, "[Byte3] bit1", "Upper consecutive offline limit exceeded")

        // Byte 4
        logBit(tag, tvr[3], 0x80, "[Byte4] bit8", "Transaction selected randomly for online processing")
        logBit(tag, tvr[3], 0x40, "[Byte4] bit7", "Merchant forced transaction online")
        logBit(tag, tvr[3], 0x20, "[Byte4] bit6", "Transaction was not performed with terminal capabilities")
        logBit(tag, tvr[3], 0x10, "[Byte4] bit5", "Reserved for use by the schemes")
        logBit(tag, tvr[3], 0x08, "[Byte4] bit4", "Reserved for use by the schemes")
        logBit(tag, tvr[3], 0x04, "[Byte4] bit3", "Reserved for use by the schemes")
        logBit(tag, tvr[3], 0x02, "[Byte4] bit2", "Reserved for use by the schemes")
        logBit(tag, tvr[3], 0x01, "[Byte4] bit1", "Reserved for use by the schemes")

        // Byte 5
        logBit(tag, tvr[4], 0x80, "[Byte5] bit8", "Default TDOL used")
        logBit(tag, tvr[4], 0x40, "[Byte5] bit7", "Issuer authentication failed")
        logBit(tag, tvr[4], 0x20, "[Byte5] bit6", "Script processing failed before final GENERATE AC")
        logBit(tag, tvr[4], 0x10, "[Byte5] bit5", "Script processing failed after final GENERATE AC")
        logBit(tag, tvr[4], 0x08, "[Byte5] bit4", "Reserved for use by the schemes")
        logBit(tag, tvr[4], 0x04, "[Byte5] bit3", "Reserved for use by the schemes")
        logBit(tag, tvr[4], 0x02, "[Byte5] bit2", "Reserved for use by the schemes")
        logBit(tag, tvr[4], 0x01, "[Byte5] bit1", "Reserved for use by the schemes")

        Log.e(tag, "==================== TVR End ====================")
    }

    private fun logBit(tag: String, b: Byte, mask: Int, bitName: String, meaning: String) {
        if ((b.toInt() and 0xFF and mask) != 0) {
            Log.e(tag, "$bitName = 1 : $meaning")
        }
    }

    fun hexToBytes(hex: String): ByteArray {
        val pure = hex.replace(" ", "")
        val len = pure.length
        val out = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            out[i / 2] = pure.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return out
    }


}

enum class EmvTag(val tag: String, val len: String) {
    COUNTRY_CODE("9F1A", "02"), // 2 bytes HEX, e.g. "0724"(Spain)
    CURRENCY_CODE("5F2A", "02"), // 2 bytes HEX, e.g. "0156"(CNY); "0840"(USD); "0978"(EUR)
    TRANSACTION_CURRENCY_EXPONENT("5F36", "01"), // 1 byte HEX, e.g. "02"
    TERMINAL_TYPE("9F35", "01"), // 1 byte HEX, e.g. "0x22"(OnlinePOS); "0x14"(OfflinePOS)
    TERMINAL_CAPABILITIES("9F33", "03"), // 3 bytes HEX, e.g. "E0F0C8"(3 * 8bits) -> CVM / Online-Offline / PaymentMethod
    ADDITIONAL_TERMINAL_CAPABILITIES("9F40", "05"), // 5 bytes HEX, e.g. "6000F0A001"
    MERCHANT_CATEGORY_CODE("9F15", "02"), // 2 bytes HEX, e.g. "7011"(Hotel); "5812"/"5814"(Restaurant); "5311"/"5411"(SuperMarket)
    TERMINAL_IDENTIFICATION_ASCII_8("9F1C", "08"), // 8 chars ASCII, e.g. "3030303030303030"(00000000). FYI, 0x30 means "0" in ASCII
    IFD_SN_ASCII_8("9F1E", "08") // 8 chars ASCII, e.g. "3030303030303030". This is SN.
}

enum class TransactionTag(val tag: String) {
    CHECK_CARD_MODE("checkCardMode"), // SWIPE; INSERT; TAP; SWIPE_OR_INSERT; SWIPE_OR_TAP; INSERT_OR_TAP; SWIPE_OR_INSERT_OR_TAP
    CURRENCY_CODE("currencyCode"), // 3 digits: "156"(CNY); "840"(USD); "978"(EUR)
    EMV_OPTION("emvOption"), // START; START_WITH_FORCE_ONLINE
    AMOUNT("amount"), // String not Int
    CASHBACK_AMOUNT("cashbackAmount"), // String not Int
    CHECK_CARD_TIMEOUT("checkCardTimeout"), // in seconds
    TRANSACTION_TYPE("transactionType"), // "00"(Purchase); "01"(Withdrawal); "09"(CashBack); "20"(Refund)
    FALLBACK_SWITCH("FallbackSwitch"), // 0: Disable; 1: Enable
    ENTER_AMOUNT_AFTER_READ_RECORD("isEnterAmtAfterReadRecord"), // If true, then need to enter the amount in onRequestSetAmount() callback.
    SUPPORT_DRL("supportDRL"), // If true. then means support Visa's Dynamic adjusting the Limit logic.
    ENABLE_BEEPER("enableBeeper"), // Enable/Disable Beeper when card is read successfully
    ENABLE_TAP_SWIPE_COLLISION("enableTapSwipeCollision"), // If enable, then will prompt when device senses TAP & SWIPE within a short period of time. If false, go directly for the first one.
    PRIORITIZED_CANDIDATE_APP("prioritizedCandidateApp"),
    DISABLE_CHECK_MSR_FORMAT("DisableCheckMSRFormat") // If true, then won't check if MagStrip Card's Track data valid or not.
}



enum class CapkTag(val tag: String) {
    RID("Rid"), // Registered Application Provider ID.
    INDEX("Index"), // The INDEX in the device to store the CAPK. It's demanded by the CA. Will be designated by the Card
    EXPONENT("Exponent"), // Public Key Component, 1 Byte
    MODULUS("Modulus"), // Public Key Modulus
    CHECKSUM("Checksum") // SHA-1 of Public Key(Exponent+Modulus), 20 Bytes
}

/*
 - The reason why TAC(Terminal Action Codes) is configured in the AID instead of Terminal Params is because it's only loaded after SELECT AID instead of beforehand
 - This also means each AID has it's own requirement of TAC.
 - Terminal Capability - Terminal Parameters; Terminal transaction strategy - TAC
 */
enum class AidTag(val tag: String) {

    AID("aid"), // ICC: e.g. "A0000000031010"(Visa Credit); "A0000000041010"(MasterCard Credit)
    APP_VERSION("appVersion"), // 9F09 e.g. "0002"
    CARD_TYPE("CardType"), // e.g. "IcCard"
    TERMINAL_FLOOR_LIMIT("terminalFloorLimit"), // Offline transaction limit - 9F1B e.g. "00000000" means not allow offline transaction
    CONTACT_TAC_DENIAL("contactTACDenial"), // 5 Bytes, will check if this applied firstly. If hit bit=1, then will decline the transaction(AAC)
    CONTACT_TAC_ONLINE("contactTACOnline"), // 5 Bytes, will check if this applied secondly. If hit bit=1, then will go online transaction(ARQC)
    CONTACT_TAC_DEFAULT("contactTACDefault"), // 5 Bytes, will check if this applied lastly. If hit bit=1, will lead to Kernel-defined behavior
    DEFAULT_DDOL("defaultDDOL"), // Dynamic Data Authentication Data Object List. Card->Terminal to ask Terminal to send Challenge data in this format. e.g. "9F3704"(4 Bytes Random Number)
    DEFAULT_TDOL("defaultTDOL"), // Terminal Data Object List. Card->Terminal to specify the Data format when Terminal wants to send GAC for TC/AAC. e.g. "9F0206"(6 Bytes Amount)
    ACQUIRER_IDENTIFIER("AcquirerIdentifier"),
    THRESHOLD_VALUE("ThresholdValue"),
    TARGET_PERCENTAGE("TargetPercentage"), // The percentage of transaction that needs to go online.
    MAX_TARGET_PERCENTAGE("MaxTargetPercentage"), // The max percentage of transaction that needs to go online. - Dynamically changed by the Kernel.
    APP_SELECT_INDICATOR("AppSelIndicator"), // The match strategy for SELECT AID (0: Full Match, 1: Partial Match)
    TRANSACTION_CURRENCY_CODE("TransactionCurrencyCode"), // The currency used for this Application
    TRANSACTION_CURRENCY_CODE_EXPONENT("TransactionCurrencyCodeExponent"), // The currency exponent used for this Application

    APPLICATION_IDENTIFIER("ApplicationIdentifier"), // PICC: e.g. "A0000000031010"(Visa Credit); "A0000000041010"(MasterCard Credit); "A000000333010101"(UnionPay International)
    TERMINAL_TRANSACTION_QUALIFIERS("TerminalTransactionQualifiers"), // 4 Bytes, PICC version of Terminal Capabilities but Dynamically loaded from Application
    TRANSACTION_LIMIT("TransactionLimit"),
    FLOOR_LIMIT("FloorLimit"),
    CVM_REQUIRED_LIMIT("CvmRequiredLimit"),
    LIMIT_SWITCH("LimitSwitch"),
    EMV_TERMINAL_FLOOR_LIMIT("EmvTerminalFloorLimit")
}

enum class CardReadMode {
    SWIPE,
    CONTACT,
    CONTACTLESS
}
