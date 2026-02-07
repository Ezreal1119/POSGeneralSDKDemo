package com.example.posgeneralsdkdemo.fragments.emv

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.posgeneralsdkdemo.EmvActivity
import com.example.posgeneralsdkdemo.PinpadActivity
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.enums.Amount
import com.example.posgeneralsdkdemo.enums.CardReadMode
import com.example.posgeneralsdkdemo.enums.Dukpt
import com.example.posgeneralsdkdemo.enums.EmvBundle
import com.example.posgeneralsdkdemo.enums.EmvOptions
import com.example.posgeneralsdkdemo.enums.FallbackSwitch
import com.example.posgeneralsdkdemo.enums.IssuerResp
import com.example.posgeneralsdkdemo.enums.PinParams
import com.example.posgeneralsdkdemo.tools.SoundTool
import com.example.posgeneralsdkdemo.tools.SoundTool.Companion.SOUND_TYPE_ERROR
import com.example.posgeneralsdkdemo.tools.SoundTool.Companion.SOUND_TYPE_SUCCESS
import com.example.posgeneralsdkdemo.utils.EmvUtil
import com.example.posgeneralsdkdemo.utils.EmvUtil.addCapkMasterCard
import com.example.posgeneralsdkdemo.utils.EmvUtil.addCapkUpi
import com.example.posgeneralsdkdemo.utils.EmvUtil.addCapkVisa
import com.example.posgeneralsdkdemo.utils.EmvUtil.analyzeTAAResult
import com.example.posgeneralsdkdemo.utils.EmvUtil.formatAmount12
import com.example.posgeneralsdkdemo.utils.EmvUtil.getCardNo
import com.example.posgeneralsdkdemo.utils.EmvUtil.hexToAscii
import com.example.posgeneralsdkdemo.utils.EmvUtil.hexToBinaryBytes
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseAip
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseAppCvmRule3Bytes
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseCid2String
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseCountryCode2String
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseCurrencyCode2String
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseTVRHits
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseTerminalType2String
import com.example.posgeneralsdkdemo.utils.EmvUtil.parseTransactionType2String
import com.example.posgeneralsdkdemo.utils.EmvUtil.updateTerminalParameters
import com.example.posgeneralsdkdemo.utils.PermissionUtil.ensureAllFilesAccess
import com.urovo.file.logfile
import com.urovo.i9000s.api.emv.ContantPara
import com.urovo.i9000s.api.emv.EmvListener
import com.urovo.i9000s.api.emv.EmvNfcKernelApi
import com.urovo.i9000s.api.emv.Funs
import com.urovo.sdk.pinpad.PinPadProviderImpl
import com.urovo.sdk.pinpad.listener.OfflinePinInputListener
import com.urovo.sdk.pinpad.listener.PinInputListener
import com.urovo.sdk.pinpad.utils.Constant
import com.urovo.sdk.utils.BytesUtil
import java.util.Hashtable
import java.util.Locale

private const val TAG = "EmvActivity_HomeFragment"
const val PIN_TRY_TIMES = "pinTryTimes"
class HomeFragment : Fragment(R.layout.fragment_emv_home) {

    private val blockingOverlay get() = requireView().findViewById<FrameLayout>(R.id.blockingOverlay)
    private val tvOverlayMsg get() = requireView().findViewById<TextView>(R.id.tvOverlayMsg)
    private val tvPinDukptReady get() = requireView().findViewById<TextView>(R.id.tvPinDukptReady)
    private val tvPinKeyReady get() = requireView().findViewById<TextView>(R.id.tvPinKeyReady)
    private val tvResult get() = requireView().findViewById<TextView>(R.id.tvResult)
    private val tvAidIccStatus get() = requireView().findViewById<TextView>(R.id.tvAidIccStatus)
    private val tvAidPiccStatus get() = requireView().findViewById<TextView>(R.id.tvAidPiccStatus)
    private val spAmount get() = requireView().findViewById<Spinner>(R.id.spAmount)
    private val spIssuerResp get() = requireView().findViewById<Spinner>(R.id.spIssuerResp)
    private val spEmvOptions get() = requireView().findViewById<Spinner>(R.id.spEmvOptions)
    private val spFallbackSwitch get() = requireView().findViewById<Spinner>(R.id.spFallbackSwitch)
    private val btnStartEmv get() = requireView().findViewById<Button>(R.id.btnStartEmv)
    private val btnStopEmv get() = requireView().findViewById<Button>(R.id.btnStopEmv)
    private val btnEnableLogOut get() = requireView().findViewById<Button>(R.id.btnEnableLogOut)
    private val btnDisableLogOut get() = requireView().findViewById<Button>(R.id.btnDisableLogOut)
    private val btnExportEmvLog get() = requireView().findViewById<Button>(R.id.btnExportEmvLog)
    private val btnDeleteEmvLog get() = requireView().findViewById<Button>(R.id.btnDeleteEmvLog)
    private val btnClearCapk get() = requireView().findViewById<Button>(R.id.btnClearCapk)
    private val btnReloadCapk get() = requireView().findViewById<Button>(R.id.btnReloadCapk)

    private val mEmvKernelManager: EmvNfcKernelApi
        get() = (requireActivity() as EmvActivity).mEmvKernelManager
    private val mPinpadManager = PinPadProviderImpl.getInstance()

    private val result = StringBuilder()
    private var enterAmountAfterReadRecordFlag: Boolean = false
    var isKernelRunning: Boolean = false
    private lateinit var backCallback: OnBackPressedCallback
    private var amountEx: Float = 0F
    private lateinit var cardReadMode: CardReadMode
    private val sharedPreferences: SharedPreferences
        get() = (requireActivity() as EmvActivity).sharedPreferences
    private lateinit var mySoundTool: SoundTool
    private val sharedVm: SharedVm by activityViewModels()
    private val arrayOfAmount = arrayOf(
        Amount.UNDER_THRESHOLD,
        Amount.MIDDLE_BETWEEN_THRESHOLD_AND_FLOOR,
        Amount.ABOVE_FLOOR,
        Amount.ENTER_AFTER_READ_RECORD)
    private val arrayOfIssuerResp = arrayOf(IssuerResp.APPROVAL, IssuerResp.DECLINE)
    private val arrayOfEmvOptions = arrayOf(EmvOptions.START, EmvOptions.START_WITH_FORCE_ONLINE)
    private val arrayOfFallbackSwitch = arrayOf(FallbackSwitch.DISABLED, FallbackSwitch.ENABLED)
    private val cvmAddition = StringBuilder()
    private var tvrBeforeTrm = ""
    private var isUnionPayIccLoaded = false
    private var isUnionPayPiccLoaded = false
    private var isVisaIccLoaded = false
    private var isVisaPiccLoaded = false
    private var isMasterCardIccLoaded = false
    private var isMasterCardPiccLoaded = false
    private var pinBlockCache: ByteArray? = null
    private var ksnCache: ByteArray? = null
    private var bypassPinPadFlag: Boolean = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mySoundTool = SoundTool.getMySound(requireContext())
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backCallback
        )

        btnStartEmv.setOnClickListener {
            if (spAmount.selectedItem == Amount.ENTER_AFTER_READ_RECORD) { // Only ICC supports enter Read Record
                onStartEmvButtonClicked(ContantPara.CheckCardMode.INSERT)
            } else {
                onStartEmvButtonClicked(ContantPara.CheckCardMode.SWIPE_OR_INSERT_OR_TAP)
            }
        }
        btnStopEmv.setOnClickListener { onStopEmvButtonClicked() }
        btnEnableLogOut.setOnClickListener { onEnableLogOutButtonClicked() }
        btnDisableLogOut.setOnClickListener { onDisableLogOutButtonClicked() }
        btnExportEmvLog.setOnClickListener { onExportEmvLogButtonClicked() }
        btnDeleteEmvLog.setOnClickListener { onDeleteEmvLogButtonClicked() }
        btnClearCapk.setOnClickListener { onClearCapkButtonClicked() }
        btnReloadCapk.setOnClickListener { onReloadCapkButtonClicked() }

        tvPinDukptReady.setOnClickListener {
            Toast.makeText(requireContext(), "Press DownloadDukpt to load Key", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), PinpadActivity::class.java))
        }
        tvPinKeyReady.setOnClickListener {
            Toast.makeText(requireContext(), "Press MK->WK to load Key", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), PinpadActivity::class.java))
        }

        spAmount.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfAmount).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spIssuerResp.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfIssuerResp).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spEmvOptions.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfEmvOptions).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spFallbackSwitch.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfFallbackSwitch).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }

        sharedVm.termParamsRefresh.observe(viewLifecycleOwner) { v ->
            if (v != null) uiRefreshOnTermParamsUpdated()
        }
        sharedVm.appParamsLoadedRefresh.observe(viewLifecycleOwner) { value ->
            if (value != null) {
                if (value in arrayOf(UNION_PAY_ICC, VISA_ICC, MASTER_CARD_ICC)) {
                    if ("No" in tvAidIccStatus.text.toString()) {
                        tvAidIccStatus.text = "AID_ICC loaded: $value; "
                        tvAidIccStatus.setTextColor(Color.GREEN)
                    } else {
                        when (value) {
                            UNION_PAY_ICC -> {
                                if (!isUnionPayIccLoaded) {
                                    tvAidIccStatus.append("$value; ")
                                }
                            }

                            VISA_ICC -> {
                                if (!isVisaIccLoaded) {
                                    tvAidIccStatus.append("$value; ")
                                }
                            }

                            MASTER_CARD_ICC -> {
                                if (!isMasterCardIccLoaded) {
                                    tvAidIccStatus.append("$value; ")
                                }
                            }
                        }
                    }
                    when (value) {
                        UNION_PAY_ICC -> isUnionPayIccLoaded = true
                        VISA_ICC -> isVisaIccLoaded = true
                        MASTER_CARD_ICC -> isMasterCardIccLoaded = true
                    }
                } else {
                    if ("No" in tvAidPiccStatus.text.toString()) {
                        tvAidPiccStatus.text = "AID_PICC loaded: $value; "
                        tvAidPiccStatus.setTextColor(Color.GREEN)
                    } else {
                        when (value) {
                            UNION_PAY_PICC -> {
                                if (!isUnionPayPiccLoaded) {
                                    tvAidPiccStatus.append("$value; ")
                                }
                            }
                            VISA_PICC -> {
                                if (!isVisaPiccLoaded) {
                                    tvAidPiccStatus.append("$value; ")
                                }
                            }
                            MASTER_CARD_PICC -> {
                                if (!isMasterCardPiccLoaded) {
                                    tvAidPiccStatus.append("$value; ")
                                }
                            }
                        }
                    }
                    when (value) {
                        UNION_PAY_PICC -> isUnionPayPiccLoaded = true
                        VISA_PICC -> isVisaPiccLoaded = true
                        MASTER_CARD_PICC -> isMasterCardPiccLoaded = true
                    }
                }
            }
        }
        sharedVm.appParamsClearRefresh.observe(viewLifecycleOwner) { v ->
            if (v != null) {
                isUnionPayIccLoaded = false
                isUnionPayPiccLoaded = false
                isVisaIccLoaded = false
                isVisaPiccLoaded = false
                isMasterCardIccLoaded = false
                isMasterCardPiccLoaded = false
                tvAidIccStatus.apply {
                    text = "No AID_ICC loaded yet!"
                    setTextColor(Color.RED)
                }
                tvAidPiccStatus.apply {
                    text = "No AID_PICC loaded yet!"
                    setTextColor(Color.RED)
                }
            }
        }

        // If "context = null", then the log file will be stored to "/sdcard/UROPE"
        // In this case, the log file will be store to "/data/data/com.example.posgeneralsdkdemo/files/UROPE"
        mEmvKernelManager.setListener(mEmvListener)
        runCatching {
            updateTerminalParameters(sharedPreferences, mEmvKernelManager)
            addCapkUpi(mEmvKernelManager)
            addCapkVisa(mEmvKernelManager)
            addCapkMasterCard(mEmvKernelManager)
        }.onSuccess {
            Toast.makeText(requireContext(), "Updated Terminal Params & Loaded CAPKs successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = buildString {
                append("<=======Terminal Params updated=======>\n\n")
                append("Terminal_Type:${sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE)}\n")
                append("Country: ${sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_TERMINAL_COUNTRY_CODE)}\n")
                append("Transaction_Currency_Exp(Fixed): 02\n")
                append("Terminal_Capabilities: ${sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES)}\n")
                append("Add_Terminal_Capabilities(Fixed): 6000F0A001\n")
                append("ThresholdRandomSwitch: ${thresholdRandomSwitchMap[sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH) ?: DEFAULT_THRESHOLD_RANDOM_SWITCH]}\n")
                append("Target Percentage(Fixed): 50\n")
                append("Max Target Percentage(Fixed): 99\n\n")
                append("<==========CAPKs loaded==========>\n\n")
                append("UPI_CAPK_INDEX:\n")
                append("[04, 08, 09, 0A, 0B]\n\n")
                append("VISA_CAPK_INDEX:\n")
                append("[08, 09, 53, 57, 92, 94, 96]\n\n")
                append("MasterCard_CAPK_INDEX:\n")
                append("[04, 05, 06, EF, F1, F3, F8, FA, FE]")
            }
        }.onFailure {
            Toast.makeText(requireContext(), "Updated Terminal Params failed", Toast.LENGTH_SHORT).show()
            tvResult.text = it.message
            it.printStackTrace()
        }

        mEmvKernelManager.updateAID(ContantPara.Operation.CLEAR, null)
        isUnionPayIccLoaded = false
        isUnionPayPiccLoaded = false
        isVisaIccLoaded = false
        isVisaPiccLoaded = false
        isMasterCardIccLoaded = false
        isMasterCardPiccLoaded = false
        tvAidIccStatus.apply {
            text = "No AID_ICC loaded yet!"
            setTextColor(Color.RED)
        }
        tvAidPiccStatus.apply {
            text = "No AID_PICC loaded yet!"
            setTextColor(Color.RED)
        }
        mEmvKernelManager.LogOutEnable(0)
        btnEnableLogOut.isEnabled = true
        btnDisableLogOut.isEnabled = false
        btnStopEmv.isEnabled = false
        hideBlockingUi()
        isKernelRunning = false
    }

    override fun onStart() {
        super.onStart()
        val ksnBuffer = ByteArray(10)
        if (mPinpadManager.DukptGetKsn(3, ksnBuffer) == 0x00 &&
            !BytesUtil.bytes2HexString(ksnBuffer).all { it == '0' }) {
            tvPinDukptReady.setBackgroundColor(Color.GREEN)
        } else {
            tvPinDukptReady.setBackgroundColor(Color.RED)
        }
        if (mPinpadManager.isKeyExist(Constant.KeyType.PIN_KEY, 99)) {
            tvPinKeyReady.setBackgroundColor(Color.GREEN)
        } else {
            tvPinKeyReady.setBackgroundColor(Color.RED)
        }
    }

    override fun onStop() {
        super.onStop()
        mEmvKernelManager.abortKernel()
    }

    private fun onStartEmvButtonClicked(cardMode: ContantPara.CheckCardMode) {
        enterAmountAfterReadRecordFlag = spAmount.selectedItem as Amount == Amount.ENTER_AFTER_READ_RECORD
        val transParams = EmvUtil.generateTransactionParameters(
            enterAmountAfterReadRecordFlag,
            (spAmount.selectedItem as Amount).amount,
            cardMode,
            (spEmvOptions.selectedItem as EmvOptions).value,
            (spFallbackSwitch.selectedItem as FallbackSwitch).value
            )
        if (spAmount.selectedItem == Amount.ENTER_AFTER_READ_RECORD) {
            Toast.makeText(requireContext(), "Please Insert Card", Toast.LENGTH_SHORT).show()
        } else if (spFallbackSwitch.selectedItem == FallbackSwitch.DISABLED) {
            Toast.makeText(requireContext(), "Please Insert/Tap/Swipe Card", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please Insert/Tap Card", Toast.LENGTH_SHORT).show()
        }
        btnStartEmv.isEnabled = false
        btnStopEmv.isEnabled = true
        tvResult.text = ""
        result.clear()
        cvmAddition.clear()
        tvrBeforeTrm = ""
        Thread {
            runCatching {
                isKernelRunning = true
                pinBlockCache = null
                ksnCache = null
                bypassPinPadFlag = false
                mEmvKernelManager.startKernel(transParams)
            }.onSuccess {
                requireActivity().runOnUiThread {
                    if (isKernelRunning) {
                        showBlockingUi()
                    }
                }
            }.onFailure {
                it.printStackTrace()
                requireActivity().runOnUiThread {
                    btnStartEmv.isEnabled = true
                    btnStopEmv.isEnabled = false
                    hideBlockingUi()
                    isKernelRunning = false
                }
            }
        }.start()
    }

    private fun onStopEmvButtonClicked() {
        runCatching {
            mEmvKernelManager.abortKernel()
        }.onSuccess {
            Toast.makeText(requireContext(), "Terminated", Toast.LENGTH_SHORT).show()
            btnStartEmv.isEnabled = true
            btnStopEmv.isEnabled = false
            hideBlockingUi()
            isKernelRunning = false
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onEnableLogOutButtonClicked() {
        runCatching {
            logfile.setLogcatOut(true)
            mEmvKernelManager.LogOutEnable(1)
        }.onSuccess {
            Toast.makeText(requireContext(), "Turn on EmvLogOut&Logcat successfully", Toast.LENGTH_SHORT).show()
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
            logfile.setLogcatOut(false)
        }.onSuccess {
            Toast.makeText(requireContext(), "Turn off EmvLogOut&Logcat successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = "Please note: \nEven tho Emv log are still output when it's disabled, those files have no valid Emv data and file sizes are very small."
            btnEnableLogOut.isEnabled = true
            btnDisableLogOut.isEnabled = false
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onExportEmvLogButtonClicked() {
        if (!ensureAllFilesAccess(requireActivity())) {
            Toast.makeText(requireContext(), "Please grant file permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val ret = EmvNfcKernelApi.exportLogFilesToExternalStorage(requireContext())
            if (!ret) throw Exception("Export Emv Log failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "Export EMV log to \"/sdcard/UROPE/\" successfully", Toast.LENGTH_SHORT).show()
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
        if (!ensureAllFilesAccess(requireActivity())) {
            Toast.makeText(requireContext(), "Please grant file permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val ret = EmvNfcKernelApi.deleteLogFiles(requireContext())
            if (!ret) throw Exception("Delete Emv Log failed")
        }.onSuccess {
            Toast.makeText(requireContext(), "Delete EMV log successfully", Toast.LENGTH_SHORT).show()
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

    private fun onClearCapkButtonClicked() {
        runCatching {
            mEmvKernelManager.updateCAPK(ContantPara.Operation.CLEAR, null)
        }.onSuccess {
            Toast.makeText(requireContext(), "Clear CAPKs successfully", Toast.LENGTH_SHORT).show()
            tvResult.text = ""
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onReloadCapkButtonClicked() {
        runCatching {
            addCapkUpi(mEmvKernelManager)
            addCapkVisa(mEmvKernelManager)
            addCapkMasterCard(mEmvKernelManager)
        }.onSuccess {
            Toast.makeText(requireContext(), "CAPKs loaded successfully", Toast.LENGTH_SHORT).show()
            uiRefreshOnCapkReloaded()
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    // <-------------------EMV methods-------------------> //

//    private fun addAidUpiIcc() { // UnionPay International
//
//        // 2. Contactless (PICC)
//        val piccAid = Hashtable<String, String>().apply {
//            put(AppTag.CARD_TYPE.tag, "UpiCard")
//            put(AppTag.APPLICATION_IDENTIFIER.tag, "A000000333010101") // UnionPay International PICC
//            put(AppTag.TERMINAL_TRANSACTION_QUALIFIERS.tag, "36004000")
//            put(AppTag.TRANSACTION_LIMIT.tag, "000005000000") // Can't process Transaction Amount above 50000.00
//            put(AppTag.FLOOR_LIMIT.tag, "000000000000") // Means always force online
//            put(AppTag.CVM_REQUIRED_LIMIT.tag, "000000030000") // Means more than 300 must need CVM
//            put(AppTag.LIMIT_SWITCH.tag, "FE00")
//            put(AppTag.EMV_TERMINAL_FLOOR_LIMIT.tag, "00000000")
//        }
//        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, piccAid)
////        mEmvKernelManager.apply {
////            iccAid.put("aid", "A000000333010102") // UnionPay International PICC
////            updateAID(ContantPara.Operation.ADD, piccAid)
////            iccAid.put("aid", "A000000333010103") // UnionPay International PICC
////            updateAID(ContantPara.Operation.ADD, piccAid)
////            iccAid.put("aid", "A000000333010106") // UnionPay International PICC
////            updateAID(ContantPara.Operation.ADD, piccAid)
////            iccAid.put("aid", "A000000333010108") // MUnionPay International PICC
////            updateAID(ContantPara.Operation.ADD, piccAid)
////        }
//    }

    private fun processOfflinePinOnce(pinEntryType: Int, emvBundle: Bundle) { // Reason of having this methods, so it can be executed recursively
        Log.e(TAG, "processOfflinePinOnce: pinEntryType=$pinEntryType, avaiablePinTryTimes:${emvBundle.getInt(PIN_TRY_TIMES)}")
        val actualAmount = if (enterAmountAfterReadRecordFlag) {
            String.format(Locale.US, "%.2f", amountEx)
        } else {
            (spAmount.selectedItem as Amount).amount
        }
        val message = when (emvBundle.getInt(PIN_TRY_TIMES)) {
            2 -> "Please enter PIN, $$actualAmount\nTwo last tries! Enter carefully!"
            1 -> "Please enter PIN, $$actualAmount\nLast try! Card might be locked if fails!"
            else -> ""
        }
        val pinpadBundle = Bundle().apply {
            // 1. Best Practice to set
            putString(PinParams.TITLE.tag, "Offline PinPad - Emv Demo") // "" by default
            putString(PinParams.MESSAGE.tag, message) // "" by default
            putString(PinParams.SUPPORT_PIN_LEN.tag, "4,6") // Will use the one set by last time by default. Thus, must set before using.
            putLong(PinParams.TIMEOUT_MS.tag, 30000) // Time out since opening the Pad. 0 by default, must set!
            putBoolean(PinParams.RANDOM_KEYBOARD.tag, false) // true by default.

            // 2. Optional
            putBoolean(PinParams.ONLINE_PIN.tag, false) // False by default. This PIN PAD is for Offline PIN
            putBoolean(PinParams.BYPASS.tag, true) // False by default. Support 0 PIN or not. Same effect as SUPPORT_PIN_LEN including 0
            putBoolean(PinParams.FULL_SCREEN.tag, true) // True by default. Won't have Cancel button when half screen
            putBoolean(PinParams.SOUND.tag, false) // False by default. Sound will be turned on when using the PinPad (lasting effect);
//            putBoolean(PinParams.RANDOM_KEYBOARD_LOCATION.value, false) // false by default. The keypad moving up & down for security reason
//            putBoolean(PinParams.INPUT_BY_SECURITY_PIN_PAD.value, false) // false by default. Set true to display the Amount.
//            putString(PinParams.INFO_LOCATION.value, "CENTER") // CENTER by default. Can change to LEFT or RIGHT
        }
        if (pinEntryType == 0) {
            pinpadBundle.putInt(PinParams.INPUT_TYPE.tag, 3) // 3: Plaintext PIN; 4: Enciphered PIN
        } else if (pinEntryType == 1) {
            pinpadBundle.putInt(PinParams.INPUT_TYPE.tag, 4) // 3: Plaintext PIN; 4: Enciphered PIN
            val publicKeyModulus = emvBundle.getByteArray(EmvBundle.PUBLIC_KEY_MODULUS.tag)
            val publicKeyModulusLen = emvBundle.getIntArray(EmvBundle.PUBLIC_KEY_MODULUS_LEN.tag)
            val publicKeyExponent = emvBundle.getByteArray(EmvBundle.PUBLIC_KEY_EXPONENT.tag)
            val publicKeyExponentLen = emvBundle.getIntArray(EmvBundle.PUBLIC_KEY_EXPONENT_LEN.tag)

            runCatching {
                val pkModulus = publicKeyModulus?.copyOf(publicKeyModulusLen!![0])
                val pkExponent = publicKeyExponent?.copyOf(publicKeyExponentLen!![0])
                pinpadBundle.apply { // ICC Public Key(Modulus&Exponent) is needed for Enciphered
                    putString(PinParams.MODULUS.tag, BytesUtil.bytes2HexString(pkModulus))
                    putInt(PinParams.MODULUS_LEN.tag, publicKeyModulusLen!![0])
                    putString(PinParams.EXPONENT.tag, BytesUtil.bytes2HexString(pkExponent))
                    putInt(PinParams.EXPONENT_LEN.tag, publicKeyExponentLen!![0])
                }
            }.onFailure {
                Toast.makeText(requireContext(), "No valid Encipher Key from ICC!", Toast.LENGTH_SHORT).show()
            }
            mPinpadManager.getOfflinePinBlock(pinpadBundle, object : OfflinePinInputListener {
                override fun onInput(pinLen: Int, key: Int) {

                }

                override fun onConfirm(resultCode: Int) {
                    Log.e(TAG, "onConfirm: resultCode=$resultCode")
                    mEmvKernelManager.sendOfflinePINVerifyResult(resultCode)
                }

                override fun onCancel(resultCode: Int) {
                    Log.e(TAG, "onCancel: resultCode=$resultCode")
                    mEmvKernelManager.sendOfflinePINVerifyResult(resultCode)
                }

                override fun onTimeOut(resultCode: Int) {
                    Log.e(TAG, "onTimeOut: resultCode=$resultCode")
                    mEmvKernelManager.sendOfflinePINVerifyResult(resultCode)
                }

                override fun onError(resultCode: Int) {
                    Log.e(TAG, "onError: resultCode=$resultCode")
                    mEmvKernelManager.sendOfflinePINVerifyResult(resultCode)
                }

                override fun onRetry(pinEntryType: Int, availableTimes: Int) { // availableTimes will decrement automatically each time Offline PinPad is called.
                    Log.e(TAG, "onRetry: pinEntryType=$pinEntryType, availableTimes=$availableTimes")
                    emvBundle.putInt(PIN_TRY_TIMES, availableTimes)
                    processOfflinePinOnce(pinEntryType, emvBundle)
                }
            })
        }
    }

    private fun showAmountInputDialog(
        context: Context,
        onConfirm: (String) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val et = EditText(context).apply {
            hint = "Enter the Amount"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        var dialog: AlertDialog? = null
        val amountEnterTimeoutHandler = Handler(Looper.getMainLooper())
        val amountEnterTimeoutRunnable = Runnable {
            dialog?.dismiss()
            onCancel?.invoke()
            Toast.makeText(requireContext(), "Enter Amount TIMEOUT", Toast.LENGTH_SHORT).show()
        }

        dialog = AlertDialog.Builder(context)
            .setTitle("Enter Amount")
            .setView(et)
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(requireContext(), "Transaction Cancelled", Toast.LENGTH_SHORT).show()
                tvResult.text = "<======Transaction Ends: No Amount Entered======>"
                amountEnterTimeoutHandler.removeCallbacks(amountEnterTimeoutRunnable)
                onCancel?.invoke()
            }
            .setPositiveButton("OK") { _, _ ->
                amountEnterTimeoutHandler.removeCallbacks(amountEnterTimeoutRunnable)
                val amount = et.text?.toString()?.trim().orEmpty()
                val v = amount.toDoubleOrNull()
                if (v == null || v <= 0) {
                    Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                    onCancel?.invoke()
                    return@setPositiveButton
                }
                val fixedAmount = String.format(Locale.US, "%.2f", v)
                onConfirm(fixedAmount)
            }.show()
        amountEnterTimeoutHandler.postDelayed(amountEnterTimeoutRunnable, 30000)
    }

    // <---------------------Listeners---------------------> //

    private val mEmvListener = object: EmvListener {
        override fun onRequestSetAmount() {
            // Call when ENTER_AMOUNT_AFTER_READ_RECORD=true and not entered any amount before kernel starts
            Log.e(TAG, "onRequestSetAmount: ")
            if (enterAmountAfterReadRecordFlag) {
                requireActivity().runOnUiThread {
                    showAmountInputDialog(
                        context = requireContext(),
                        onConfirm = { amount ->
                            amountEx = amount.toFloat()
                            mEmvKernelManager.setAmountEx(amount, null)
                        },
                        onCancel = {
                            mEmvKernelManager.abortKernel()
                            btnStartEmv.isEnabled = true
                            btnStopEmv.isEnabled = false
                            hideBlockingUi()
                            isKernelRunning = false
                        }
                    )
                }
            }
        }

        override fun onReturnCheckCardResult(
            checkCardResult: ContantPara.CheckCardResult,
            hashtable: Hashtable<String?, String?>
        ) {
            Log.e(TAG, "onReturnCheckCardResult: cardResult=$checkCardResult")
            when (checkCardResult) {
                ContantPara.CheckCardResult.MSR -> {
                    // MSR is not part of EMV, this will end the Kernel and APP will handle the uplink to Issuer.
                    cardReadMode = CardReadMode.SWIPE
                    val stripeInfo = EmvUtil.parseStripeInfo(hashtable.get("StripInfo")!!)
                    val track1Info = EmvUtil.parseTrack1(stripeInfo.track1 ?: "")
                    val track2Info = EmvUtil.parseTrack2(stripeInfo.track2 ?: "")
                    requireActivity().runOnUiThread {
                        tvResult.text = buildString {
                            append("<=========Magnetic Stripe Card=========>\n\n")
                            append("CardNo: ${hashtable.get("CardNo")}\n\n")
                            append("TRACK_1: ${stripeInfo.track1}\n")
                            append(" - PAN: ${track1Info?.pan}\n")
                            append(" - Name: ${track1Info?.name}\n")
                            append(" - ExpiryDate: ${track1Info?.expiryYYMM}\n")
                            append(" - ServiceCode: ${track1Info?.serviceCode}\n")
                            append(" - Discretionary: ${track1Info?.discretionary}\n\n")
                            append("TRACK_2: ${stripeInfo.track2}\n")
                            append(" - PAN: ${track2Info?.pan}\n")
                            append(" - ExpiryDate: ${track2Info?.expiryYYMM}\n")
                            append(" - ServiceCode: ${track2Info?.serviceCode}\n")
                            append(" - Discretionary: ${track2Info?.discretionary}\n\n")
                            append("TRACK_3: ${stripeInfo.track3}\n")
                            append("(Please refer to Issuer for more Info)\n\n")
                            append("<==========Transaction Ends==========>")
                            btnStartEmv.isEnabled = true
                            btnStopEmv.isEnabled = false
                            hideBlockingUi()
                            isKernelRunning = false
                        }
                    }
                }
                ContantPara.CheckCardResult.INSERTED_CARD -> { // ICC(Contact)
                    cardReadMode = CardReadMode.CONTACT
                    result.apply {
                        append("<===========Card Detected===========>\n\n")
                        append("Card Type: ICCard(EMV_Contact)\n\n")
                    }
                    requireActivity().runOnUiThread { Toast.makeText(requireContext(), "ICC Card detected", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.TAP_CARD_DETECTED -> { // PICC(Contactless)
                    cardReadMode = CardReadMode.CONTACTLESS
                    result.apply {
                        append("<===========Card Detected===========>\n\n")
                        append("Card Type: PICCard(EMV_Contactless)\n\n")
                    }
                    requireActivity().runOnUiThread { Toast.makeText(requireContext(), "PICC Card detected", Toast.LENGTH_SHORT).show() }
                }
                ContantPara.CheckCardResult.NEED_FALLBACK -> {
                    // Can be triggered if anything went wrong with ICCard when FALLBACK_SWITCH=1. e.g. (a). Present Any Rectangle thing (b). no related-AID loaded for the ICCard
                    // Because startKernel() is called again(SWIPE_OR_TAP). Kernel will look for PICC or MSC.
                    // MSC won't be block by FALLBACK_SWITCH in this case
                    Log.e(TAG, "onReturnCheckCardResult: NEED_FALLBACK")
                    do {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "NEED_FALLBACK. Please remove the ICCard", Toast.LENGTH_SHORT).show()
                        }
                    } while (!mEmvKernelManager.CheckCardIsOut(5000))
                    requireActivity().runOnUiThread {
                        val transParams = EmvUtil.generateTransactionParameters(
                            enterAmountAfterReadRecordFlag,
                            (spAmount.selectedItem as Amount).amount,
                            ContantPara.CheckCardMode.SWIPE_OR_TAP,
                            (spEmvOptions.selectedItem as EmvOptions).value,
                            (spFallbackSwitch.selectedItem as FallbackSwitch).value
                        )
                        Thread {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "Please Tap/Swipe Card", Toast.LENGTH_SHORT).show()
                            }
                            mEmvKernelManager.startKernel(transParams)
                        }.start()
                    }
                }
                ContantPara.CheckCardResult.USE_ICC_CARD -> {
                    // Can be triggered if present Mag Card when TRANS_FALLBACK_SWITCH=1.
                    // Because startKernel() is called again(SWIPE_OR_TAP). Kernel will look for PICC or MSC.
                    // This will terminal the kernel (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: USE_ICC_CARD")
                    requireActivity().runOnUiThread {
                        requireActivity().runOnUiThread {
                            val transParams = EmvUtil.generateTransactionParameters(
                                enterAmountAfterReadRecordFlag,
                                (spAmount.selectedItem as Amount).amount,
                                ContantPara.CheckCardMode.INSERT,
                                (spEmvOptions.selectedItem as EmvOptions).value,
                                (spFallbackSwitch.selectedItem as FallbackSwitch).value)
                            Thread {
                                requireActivity().runOnUiThread {
                                    Toast.makeText(requireContext(), "Please insert Card (FALLBACK on)", Toast.LENGTH_SHORT).show()
                                }
                                mEmvKernelManager.startKernel(transParams)
                            }.start()
                        }
                    }
                }
                ContantPara.CheckCardResult.BAD_SWIPE -> {
                    // Can be triggered by a very bad SWIPE
                    // This will end the Kernel (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: BAD_SWIPE")
                    requireActivity().runOnUiThread {
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                        hideBlockingUi()
                        isKernelRunning = false
                        tvResult.text = buildString {
                            append("<======Transaction Ends: BAD_SWIPE======>\n\n")
                            append("This can be triggered by a very bad SWIPE")
                        }
                    }
                }
                ContantPara.CheckCardResult.NOT_ICC -> {
                    // Can be triggered by present Any Rectangle thing(TRANS_FALLBACK_SWITCH=0)
                    // This will end the Kernel immediately after removing the card (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: NOT_ICC")
                    do { // 10s is the period to toast
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "NOT_ICC. Please remove the ICCard", Toast.LENGTH_SHORT).show()
                        }
                    } while (!mEmvKernelManager.CheckCardIsOut(5000))
                    requireActivity().runOnUiThread {
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                        hideBlockingUi()
                        isKernelRunning = false
                        tvResult.text = buildString {
                            append("<======Transaction Ends: NOT_ICC======>\n\n")
                            append("This can be triggered by present anything that triggers the Reader's sensor.\n")
                            append("e.g. SLE card; Name Card, and etc.\n\n")
                            append("Please note:\n")
                            append("This will only be triggered if TRANS_FALLBACK_SWITCH=0\n")
                            append("if TRANS_FALLBACK_SWITCH=1, NEED_FALLBACK will be called")
                        }
                    }
                }
                ContantPara.CheckCardResult.TIMEOUT -> {
                    // Can be triggered by not presenting any card. The TIMEOUT can be set via transParameters
                    // This will end the Kernel (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: TIMEOUT")
                    requireActivity().runOnUiThread {
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                        hideBlockingUi()
                        isKernelRunning = false
                        tvResult.text = buildString {
                            append("<======Transaction Ends: TIMEOUT======>\n\n")
                            append("This can be triggered if didn't present any Card within the countdown")
                        }
                    }
                }
                ContantPara.CheckCardResult.CANCEL -> {
                    // Can be triggered by pressing btnStopEmv
                    // This will end the Kernel (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: CANCEL")
                    requireActivity().runOnUiThread {
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                        hideBlockingUi()
                        isKernelRunning = false
                        tvResult.text = buildString {
                            append("<======Transaction Ends: CANCEL======>\n\n")
                            append("This can be triggered if abortKernel() is called")
                        }
                    }
                }
                ContantPara.CheckCardResult.DEVICE_BUSY -> {
                    // This will end the Kernel (without calling abortKernel())
                    Log.e(TAG, "onReturnCheckCardResult: DEVICE_BUSY")
                    requireActivity().runOnUiThread {
                        btnStartEmv.isEnabled = true
                        btnStopEmv.isEnabled = false
                        hideBlockingUi()
                        isKernelRunning = false
                        tvResult.text = buildString {
                            append("<======Transaction Ends: DEVICE_BUSY======>\n\n")
                            append("This can be triggered if Reading modules are in used")
                        }
                    }
                }
                ContantPara.CheckCardResult.MULT_CARD -> {
                    // Can be triggered by tapping more than one PICCs
                    // This won't terminate the kernel. Thus, no need to re-call startKernel()
                    Log.e(TAG, "onReturnCheckCardResult: MULT_CARD")
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "MULT_CARD! Please try again!", Toast.LENGTH_SHORT).show()
                        result.apply {
                            append("<========MULT_CARD Detected========>\n\n")
                        }
                    }
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
            cvmAddition.append("CVM_OnlinePIN: ")
            val actualAmount = if (enterAmountAfterReadRecordFlag) {
                String.format(Locale.US, "%.2f", amountEx)
            } else {
                (spAmount.selectedItem as Amount).amount
            }
            val pinpadBundle = Bundle().apply {
                // 1. Best practice to set
                putString(PinParams.CARD_NO.tag, getCardNo(mEmvKernelManager.getValByTag(0x5A), mEmvKernelManager.getValByTag(0x57))) // The field is a Must for generating PINBlock
                putString(PinParams.TITLE.tag, "Online PinPad - Emv Demo") // "" by default
                putString(PinParams.MESSAGE.tag, "Please enter PIN, $$actualAmount") // "" by default
                putString(PinParams.SUPPORT_PIN_LEN.tag, "4,6") // Will use the one set by last time by default. Thus, must set before using.
                putLong(PinParams.TIMEOUT_MS.tag, 30000) // Time out since opening the Pad. 0 by default, must set!
                putBoolean(PinParams.RANDOM_KEYBOARD.tag, false) // true by default.

                // 2. Optional:
                putBoolean(PinParams.BYPASS.tag, true) // False by default. Support 0 PIN or not. Same effect as SUPPORT_PIN_LEN including 0
                putBoolean(PinParams.FULL_SCREEN.tag, true) // true by default. Won't have Cancel button when half screen
                putBoolean(PinParams.ONLINE_PIN.tag, true) // True by default. This is the PIN PAD for Online PIN
                putBoolean(PinParams.SOUND.tag, false) // False by default. Sound will be turned on when using the PinPad (lasting effect)
//                putBoolean(PinParams.RANDOM_KEYBOARD_LOCATION.value, false) // false by default. The keypad moving up & down for security reason
//                putBoolean(PinParams.INPUT_BY_SECURITY_PIN_PAD.value, false) // false by default. Set true to display the Amount.
//                putString(PinParams.INFO_LOCATION.value, "CENTER") // CENTER by default. Can change to LEFT or RIGHT
            }
            if (pinEntrySource == ContantPara.PinEntrySource.KEYPAD) {
                if (mPinpadManager.DukptGetKsn(Dukpt.PIN.index, ByteArray(10)) == 0x00) {
                    Log.e(TAG, "onRequestPinEntry: DUKPT Pinpad was called")
                    cvmAddition.append("DUKPT Pinpad was called\n\n")
                    pinpadBundle.putInt(PinParams.PIN_KEY_NO.tag, Dukpt.PIN.index) // Must set, will call onError otherwise
                    mPinpadManager.GetDukptPinBlock(pinpadBundle, mPinInputListener)
                } else {
                    Log.e(TAG, "onRequestPinEntry: MK/SK Pinpad was called")
                    cvmAddition.append("MK/SK Pinpad was called\n\n")
                    pinpadBundle.putInt(PinParams.PIN_KEY_NO.tag, 99) // The keySlot of the PIN_KEY to use for encryption. Must Set since onlinePin must be encrypted!
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
            val tagList = listOf("5A", "57")
            val ret = mEmvKernelManager.getTlvByTagLists(tagList)
            Log.e(TAG, "onRequestConfirmCardno: $ret", )
            result.apply {
                append("<==========Terminal Params==========>\n\n")
//                append("IFD SN ASCII: ${hexToAscii(mEmvKernelManager.getValByTag(0x9F1E))}\n")
//                append("TID ASCII: ${hexToAscii(mEmvKernelManager.getValByTag(0x9F1C))}\n")
//                append("Merchant Category Code: ${mEmvKernelManager.getValByTag(0x9F15)} - ${parseMerchantCategoryCode2String(mEmvKernelManager.getValByTag(0x9F15))}\n")
                append("Terminal Type: ${mEmvKernelManager.getValByTag(0x9F35)} - ${parseTerminalType2String(mEmvKernelManager.getValByTag(0x9F35))}\n")
                append("Country Code: ${mEmvKernelManager.getValByTag(0x9F1A)} - ${parseCountryCode2String(mEmvKernelManager.getValByTag(0x9F1A))}\n")
                append("Currency Exponent: ${mEmvKernelManager.getValByTag(0x5F36)}\n")
                append("Terminal Capabilities: ${mEmvKernelManager.getValByTag(0x9F33)}\n")
                append("Add_Terminal_Capabilities: ${mEmvKernelManager.getValByTag(0x9F40)}\n")
                append("ThresholdRandomSwitch: ${thresholdRandomSwitchMap[sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH) ?: DEFAULT_THRESHOLD_RANDOM_SWITCH]}\n")
                append("Target Percentage(Fixed): 50\n")
                append("Max Target Percentage(Fixed): 99\n\n")

                append("Threshold(App): ${formatAmount12(mEmvKernelManager.getValByTag(0xDF15), 2)}\n")
                append("Floor Limit(App): ${formatAmount12(mEmvKernelManager.getValByTag(0x9F1B), 2)}\n")
                append("TAC_DENIAL(App): ${mEmvKernelManager.getValByTag(0xDF13)}\n")
                append("TAC_ONLINE(App): ${mEmvKernelManager.getValByTag(0xDF12)}\n")
                append("TAC_DEFAULT(APP)(Offline): ${mEmvKernelManager.getValByTag(0xDF11)}\n\n")

            }
            result.apply {
                append("<========Transaction Info========>\n\n")
                append("Transaction Type: ${mEmvKernelManager.getValByTag(0x9C)} - ${parseTransactionType2String(mEmvKernelManager.getValByTag(0x9C))}\n")
                append("Transaction Amount: ${formatAmount12(mEmvKernelManager.getValByTag(0x9F02), mEmvKernelManager.getValByTag(0x5F36).toInt())}\n")
                append("TransactionCurrency Code: ${mEmvKernelManager.getValByTag(0x5F2A)} - ${parseCurrencyCode2String(mEmvKernelManager.getValByTag(0x5F2A))}\n")
                append("Transaction Date: ${mEmvKernelManager.getValByTag(0x9A)}\n\n")
            }
            result.apply {
                append("<============GPO Started============>\n\n")

                append("App PAN: ${getCardNo(mEmvKernelManager.getValByTag(0x5A), mEmvKernelManager.getValByTag(0x57))}\n")
                append("App Track_2: ${mEmvKernelManager.getValByTag(0x57)}\n")
                append("App ID(AID): ${mEmvKernelManager.getValByTag(0x4F)}\n")
                append("App Ascii Name: ${hexToAscii(mEmvKernelManager.getValByTag(0x50))}\n")
                append("App Priority: ${mEmvKernelManager.getValByTag(0x87)}\n")
                append("App Expiration Date: ${mEmvKernelManager.getValByTag(0x5F24)}\n")
                append("App Effective Date: ${mEmvKernelManager.getValByTag(0x5F25)}\n")
                append("App Issuer Country Code: ${mEmvKernelManager.getValByTag(0x5F28)} - ${parseCountryCode2String(mEmvKernelManager.getValByTag(0x5F28))}\n")
                append("App Currency Code: ${mEmvKernelManager.getValByTag(0x9F42)} - ${parseCurrencyCode2String(mEmvKernelManager.getValByTag(0x9F42))}\n\n")
                append("AIP(Application Interchange Profile): ${mEmvKernelManager.getValByTag(0x82)}\n")
                append("${parseAip(mEmvKernelManager.getValByTag(0x82))}\n\n")
                append("App Usage Control(AUC): ${mEmvKernelManager.getValByTag(0x9F07)}\n")
                append("App CVM: ${mEmvKernelManager.getValByTag(0x8E)}\n")
                append("(1: P_OFF_PIN; 2: E_ON_PIN; 4: E_OFF_PIN; E: Signature; F: No CVM)\n")
                append("App DDOL: ${mEmvKernelManager.getValByTag(0x9F49)}\n\n")
            }
            // Confirm if cardNo is not null and not blank
            mEmvKernelManager.sendConfirmCardnoResult(mEmvKernelManager.getValByTag(0x5A).isNotBlank())
        }

        override fun onRequestFinalConfirm() {
            // Right before TRM
            Log.e(TAG, "onRequestFinalConfirm: CVM Stage, before TRM")
            tvrBeforeTrm = mEmvKernelManager.getValByTag(0x95)
            result.append("<============ODA Started============>\n\n")
            if ((mEmvKernelManager.getValByTag(0x95).substring(0, 2).toInt(16) and 0x80) != 0) {
                result.append(" - ODA not performed\n\n")
            } else if ((mEmvKernelManager.getValByTag(0x95).substring(0, 2).toInt(16) and 0x02) != 0) {
                result.append(" - SDA triggered\n\n")
            } else if (mEmvKernelManager.getValByTag(0x9F4B).isNotEmpty()) {
                result.append(" - DDA triggered\n\n")
            } else {
                result.append(" - CDA triggered\n\n")
            }

            result.append("<=============PR Started=============>\n\n")
            result.append(" - AUC: ${mEmvKernelManager.getValByTag(0x9F07)}\n\n")

            result.apply {
                result.append("<=============CVM Started=============>\n\n")
                append("CVM Result:")
                append("${mEmvKernelManager.getValByTag(0x9F34)}\n")
                append("${parseAppCvmRule3Bytes(mEmvKernelManager.getValByTag(0x9F34))}\n\n")
                append(cvmAddition)
            }
            mEmvKernelManager.sendFinalConfirmResult(true)
        }

        override fun onRequestOnlineProcess(cardTlvData: String?, dataKsn: String?) {
            // This is After getting ARQC(80) from GAC-1_Resp
            // Form ISO8583(DE55_PINBlock_MAC) and send to Issuer, then forward the ARPC in GAC-2 to ICC
            // This is a simulation of the Issuer returning the Approval Data
            Log.e(TAG, "onRequestOnlineProcess: Sending ISO8583 and get ARPC from Issuer")
            result.apply {
                append("<============TRM Started============>\n\n")
                append("$tvrBeforeTrm -> ${mEmvKernelManager.getValByTag(0x95)}\n\n")

                append("<============TAA Started============>\n\n")
                append("TAA result: \n")
                append("[${analyzeTAAResult(
                        tvr = mEmvKernelManager.getValByTag(0x95),
                        tacDenial = mEmvKernelManager.getValByTag(0xDF13),
                        iacDenial = mEmvKernelManager.getValByTag(0x9F0E),
                        tacOnline = mEmvKernelManager.getValByTag(0xDF12),
                        iacOnline = mEmvKernelManager.getValByTag(0x9F0F),
                        tacDefault = mEmvKernelManager.getValByTag(0xDF11),
                        iacDefault = mEmvKernelManager.getValByTag(0x9F0D)
                )}]\n\n")
                append("TVR: ${mEmvKernelManager.getValByTag(0x95)}\n")
                append("${parseTVRHits(mEmvKernelManager.getValByTag(0x95))}\n\n")
                append("TAC_DENIAL: ${mEmvKernelManager.getValByTag(0xDF13)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF13))}\n")
                append("IAC_DENIAL: ${mEmvKernelManager.getValByTag(0x9F0E)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0E))}\n")
                append("TAC_ONLINE: ${mEmvKernelManager.getValByTag(0xDF12)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF12))}\n")
                append("IAC_ONLINE: ${mEmvKernelManager.getValByTag(0x9F0F)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0F))}\n")
                append("TAC_DEFAULT(OFFLINE): ${mEmvKernelManager.getValByTag(0xDF11)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF11))}\n")
                append("IAC_DEFAULT(OFFLINE): ${mEmvKernelManager.getValByTag(0x9F0D)}\n")
                append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0D))}\n\n")
                append("<=========GAC-1 Started=========>\n\n")
                append("GAC-1 CDOL1(8C): ")
                append("${mEmvKernelManager.getValByTag(0x8C)}\n")
                append("GAC-1_Resp CID(9F27): ")
                append("${mEmvKernelManager.getValByTag(0x9F27)} - ${parseCid2String(mEmvKernelManager.getValByTag(0x9F27))}")
                append("\nGAC-1_Resp ATC(9F36): ")
                append(mEmvKernelManager.getValByTag(0x9F36))
                append("\nGAC-1_Resp AC(9F26) - ARQC/TC/AAC:\n")
                append(mEmvKernelManager.getValByTag(0x9F26))
                append("\nSDAD: ${mEmvKernelManager.getValByTag(0x9F4B)}\n")
                append("\n\n")
            }
            result.apply {
                append("<===========ISO8583===========>\n\n")
                append("Field 55: $cardTlvData\n\n")
                if (ksnCache != null) append("KSN(F60/F61/F62/F63): ${BytesUtil.bytes2HexString(ksnCache)}\n")
                if (pinBlockCache != null) append("PINBlock(F52): ${BytesUtil.bytes2HexString(pinBlockCache)}\n\n")
            }
            var authorizationResponseCode: String
            var authorizationCode: String
            result.append("<=========Got Issuer_Resp=========>\n\n")
            if (spIssuerResp.selectedItem as IssuerResp == IssuerResp.APPROVAL) {
                authorizationResponseCode = "8A023030" // ARC ["00"(3030): Approved; "05"(3035): Declined; "51"(3531): Insufficient balance]
                authorizationCode = "8906${Funs.convertStringToHex("000001")}" // Approved Serial Number: "89" is the Prefix
                result.apply {
                    append("ISO8583_Resp ARC(8A): 3030 - Approval")
                    append("\nISO8583_Resp AC(89): 303030303031\n\n")
                }
            } else {
                authorizationResponseCode = "8A023035" // ARC ["00"(3030): Approved; "05"(3035): Declined; "51"(3531): Insufficient balance]
                authorizationCode = "" // No AC for Issuer Decline normally
                result.append("ISO8583_Resp ARC(8A): 3035 - Decline\n\n")
            }
            mEmvKernelManager.sendOnlineProcessResult(true, authorizationResponseCode + authorizationCode)
        }

        override fun onReturnBatchData(cardTlvData: String?) {
            Log.e(TAG, "onReturnBatchData: ")
            TODO("Not yet implemented")
        }

        override fun onReturnTransactionResult(transactionResult: ContantPara.TransactionResult?) {
            when (transactionResult) {
                ContantPara.TransactionResult.ONLINE_APPROVAL, ContantPara.TransactionResult.ONLINE_DECLINED -> {
                    result.apply {
                        append("<=========GAC-2 Started=========>\n\n")
                        append("GAC-2 CDOL2(8D): ")
                        append("${mEmvKernelManager.getValByTag(0x8D)}\n")
                        if ("91" in mEmvKernelManager.getValByTag(0x8D)) {
                            append("(ICC needs ARPC(91) for Issuer Authentication)\n")
                        }
                        append("GAC-2_Resp CID(9F27): ")
                        append("${mEmvKernelManager.getValByTag(0x9F27)} - ${parseCid2String(mEmvKernelManager.getValByTag(0x9F27))}")
                        append("\nGAC-2_Resp ATC(9F36): ")
                        append(mEmvKernelManager.getValByTag(0x9F36))
                        append("\nGAC-2_Resp AC(9F26) - AAC/TC: \n")
                        append(mEmvKernelManager.getValByTag(0x9F26))
                        append("\n\n")
                        append("Please note:\n")
                        append("If 8A=3030, but AAC from ICC. The app must send an authorization Reversal to Issuer for unfreeze the Money Hold(not clear) as per the regulation of EMV L3.")
                        append("\n\n")
                    }
                }
                ContantPara.TransactionResult.OFFLINE_DECLINED, ContantPara.TransactionResult.OFFLINE_APPROVAL -> {
                    result.apply {
                        append("<============TRM Started============>\n\n")
                        append(".........\n\n")

                        append("<============TAA Started============>\n\n")
                        append("TAA result: \n")
                        append("[${analyzeTAAResult(
                            tvr = mEmvKernelManager.getValByTag(0x95),
                            tacDenial = mEmvKernelManager.getValByTag(0xDF13),
                            iacDenial = mEmvKernelManager.getValByTag(0x9F0E),
                            tacOnline = mEmvKernelManager.getValByTag(0xDF12),
                            iacOnline = mEmvKernelManager.getValByTag(0x9F0F),
                            tacDefault = mEmvKernelManager.getValByTag(0xDF11),
                            iacDefault = mEmvKernelManager.getValByTag(0x9F0D)
                        )}]\n\n")
                        append("TVR: ${mEmvKernelManager.getValByTag(0x95)}\n")
                        append("${parseTVRHits(mEmvKernelManager.getValByTag(0x95))}\n\n")
                        append("TAC_DENIAL: ${mEmvKernelManager.getValByTag(0xDF13)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF13))}\n")
                        append("IAC_DENIAL: ${mEmvKernelManager.getValByTag(0x9F0E)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0E))}\n")
                        append("TAC_ONLINE: ${mEmvKernelManager.getValByTag(0xDF12)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF12))}\n")
                        append("IAC_ONLINE: ${mEmvKernelManager.getValByTag(0x9F0F)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0F))}\n")
                        append("TAC_DEFAULT(OFFLINE): ${mEmvKernelManager.getValByTag(0xDF11)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0xDF11))}\n")
                        append("IAC_DEFAULT(OFFLINE): ${mEmvKernelManager.getValByTag(0x9F0D)}\n")
                        append(" - ${hexToBinaryBytes(mEmvKernelManager.getValByTag(0x9F0D))}\n\n")

                        append("<===========GAC-1 Started===========>\n\n")
                        append("GAC-1 CDOL1(8C): ")
                        append("${mEmvKernelManager.getValByTag(0x8C)}\n")
                        append("GAC-1_Resp CID(9F27): ")
                        append("${mEmvKernelManager.getValByTag(0x9F27)} - ${parseCid2String(mEmvKernelManager.getValByTag(0x9F27))}")
                        append("\nGAC-1_Resp ATC(9F36): ")
                        append(mEmvKernelManager.getValByTag(0x9F36))
                        append("\nGAC-1_Resp AC(9F26) - AAC/TC: \n")
                        append(mEmvKernelManager.getValByTag(0x9F26))
                        append("\nSDAD: ${mEmvKernelManager.getValByTag(0x9F4B)}\n\n")
                    }
                }
                else -> null
            }
            result.append("<===========Transaction End===========>")

            requireActivity().runOnUiThread {
                btnStartEmv.isEnabled = true
                btnStopEmv.isEnabled = false
                hideBlockingUi()
                isKernelRunning = false
                result.insert(0, "<=======$transactionResult=======>\n\n")
                tvResult.text = result
                Toast.makeText(requireContext(), transactionResult.toString(), Toast.LENGTH_SHORT).show()
            }
            Log.e(TAG, "onReturnTransactionResult: $transactionResult")
        }

        override fun onRequestDisplayText(displayText: ContantPara.DisplayText?) {
            Log.e(TAG, "onRequestDisplayText: $displayText")
            requireActivity().runOnUiThread { Toast.makeText(requireContext(), displayText.toString(), Toast.LENGTH_SHORT).show() }
        }

        override fun onRequestOfflinePINVerify(
            pinEntrySource: ContantPara.PinEntrySource?,
            pinEntryType: Int,
            emvBundle: Bundle
        ) {
            // CVM - offlinePIN: will callback this method
            Log.e(TAG, "onRequestOfflinePINVerify: ")
            emvBundle.putInt(PIN_TRY_TIMES, mEmvKernelManager.offlinePinTryTimes)
            processOfflinePinOnce(pinEntryType, emvBundle)
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
            Log.e(TAG, "onNFCrequestTipsConfirm: msgID=${msgID}, msg=$msg")
            result.apply {
                append("<========PICC Comm finished========>\n\n ")
                append("Msg: $msg\n")
                append("Kernel ID: ${mEmvKernelManager.getValByTag(0x9F2A)}\n")
                append("(Already got CID/AC from PICC at this point)\n\n")
                append("<=========Terminal Parameters=========>\n\n")
                append("Terminal Type: ${mEmvKernelManager.getValByTag(0x9F35)} - ${parseTerminalType2String(mEmvKernelManager.getValByTag(0x9F35))}\n")
                append("Country Code: ${mEmvKernelManager.getValByTag(0x9F1A)} - ${parseCountryCode2String(mEmvKernelManager.getValByTag(0x9F1A))}\n")
                append("Currency Exponent: ${mEmvKernelManager.getValByTag(0x5F36)}\n")
                append("Terminal Transaction Qualifier(App): ${mEmvKernelManager.getValByTag(0x9F66)}\n\n")
                append("<==========Transaction Info==========>\n\n")
                append("Transaction Type: ${mEmvKernelManager.getValByTag(0x9C)} - ${parseTransactionType2String(mEmvKernelManager.getValByTag(0x9C))}\n")
                append("Transaction Amount: ${formatAmount12(mEmvKernelManager.getValByTag(0x9F02), mEmvKernelManager.getValByTag(0x5F36).toInt())}\n")
                append("TransactionCurrency Code: ${mEmvKernelManager.getValByTag(0x5F2A)} - ${parseCurrencyCode2String(mEmvKernelManager.getValByTag(0x5F2A))}\n")
                append("Transaction Date: ${mEmvKernelManager.getValByTag(0x9A)}\n\n")
                append("<============App Info============>\n\n")
                append("App PAN: ${getCardNo(mEmvKernelManager.getValByTag(0x5A), mEmvKernelManager.getValByTag(0x57))}\n")
                append("App Track_2: ${mEmvKernelManager.getValByTag(0x57)}\n")
                append("App ID(AID): ${mEmvKernelManager.getValByTag(0x4F)}\n")
                append("App Ascii Name: ${hexToAscii(mEmvKernelManager.getValByTag(0x50))}\n")
                append("App Priority: ${mEmvKernelManager.getValByTag(0x87)}\n")
                append("App Expiration Date: ${mEmvKernelManager.getValByTag(0x5F24)}\n")
                append("App Effective Date: ${mEmvKernelManager.getValByTag(0x5F25)}\n")
                append("AIP(Application Interchange Profile): ${mEmvKernelManager.getValByTag(0x82)}\n")
                append("${parseAip(mEmvKernelManager.getValByTag(0x82))}\n\n")
                append("<==========GPO_Resp Info==========>\n\n")
                append("GPO_Resp CID(9F27): ")
                append("${mEmvKernelManager.getValByTag(0x9F27)} - ${parseCid2String(mEmvKernelManager.getValByTag(0x9F27))}")
                append("\nGPO_Resp ATC(9F36): ")
                append(mEmvKernelManager.getValByTag(0x9F36))
                append("\nGPO_Resp AC(9F26) - ARQC/TC/AAC:\n")
                append(mEmvKernelManager.getValByTag(0x9F26))
                append("\n\n")
            }
            when (msgID) {
                ContantPara.NfcTipMessageID.CARD_READ_OK -> { // Play this sound if Read Card successfully
                    SoundTool.getMySound(requireContext()).playSound(SOUND_TYPE_SUCCESS)
                }
                ContantPara.NfcTipMessageID.END_APPLICATION -> { // Play this sound if Read Card failed
                    SoundTool.getMySound(requireContext()).playSound(SOUND_TYPE_ERROR)
                }
                else -> null
            }

        }

        override fun onReturnNfcCardData(hashtable: Hashtable<String?, String?>?) {
            Log.e(TAG, "onReturnNfcCardData: hashTable=$hashtable")
            result.apply {
                append("<========ODA/CVM/TAA finished========>\n\n")
                append("Field 55: ${hashtable?.get("EMVDATA")}\n\n")
                if (ksnCache != null) append("KSN(F60/F61/F62/F63): ${BytesUtil.bytes2HexString(ksnCache)}\n")
                if (pinBlockCache != null) append("PINBlock(F52): ${BytesUtil.bytes2HexString(pinBlockCache)}\n\n")
            }
        }

        override fun onNFCrequestOnline() {
            Log.e(TAG, "onNFCrequestOnline: ")
            result.apply {
                append("<===========ISO8583===========>\n\n")
                append("Preparing for other fields of ISO8583...\n\n")
            }
            var authorizationResponseCode: String
            var authorizationCode: String
            result.append("<=========Got Issuer_Resp=========>\n\n")
            if (spIssuerResp.selectedItem as IssuerResp == IssuerResp.APPROVAL) {
                authorizationResponseCode = "8A023030" // ARC ["00"(3030): Approved; "05"(3035): Declined; "51"(3531): Insufficient balance]
                authorizationCode = "8906${Funs.convertStringToHex("000001")}" // Approved Serial Number: "89" is the Prefix
                result.apply {
                    append("ISO8583_Resp ARC(8A): 3030 - Approval\n")
                    append("ISO8583_Resp AC(89): 303030303031\n\n")
                }
            } else {
                authorizationResponseCode = "8A023035" // ARC ["00"(3030): Approved; "05"(3035): Declined; "51"(3531): Insufficient balance]
                authorizationCode = "" // No AC for Issuer Decline normally
                result.append("ISO8583_Resp ARC(8A): 3035 - Decline\n\n")
            }
            mEmvKernelManager.sendOnlineProcessResult(true, authorizationResponseCode + authorizationCode)
        }

        override fun onNFCrequestImportPin(type: Int, lastTimeFlag: Int, amt: String?) {
            // CVM OnlinePIN for NFC(PICC)
            cvmAddition.append("CVM_OnlinePIN: ")
            val actualAmount = if (enterAmountAfterReadRecordFlag) {
                String.format(Locale.US, "%.2f", amountEx)
            } else {
                (spAmount.selectedItem as Amount).amount
            }
            val pinpadBundle = Bundle().apply {
                // 1. Best practice to have
                putString(PinParams.CARD_NO.tag, getCardNo(mEmvKernelManager.getValByTag(0x5A), mEmvKernelManager.getValByTag(0x57))) // The field is a Must for generating PINBlock
                putString(PinParams.TITLE.tag, "Online PinPad - Emv Demo") // "" by default
                putString(PinParams.MESSAGE.tag, "Please enter PIN, $$actualAmount") // "" by default
                putString(PinParams.SUPPORT_PIN_LEN.tag, "4,6") // Will use the one set by last time by default. Thus, must set before using.
                putLong(PinParams.TIMEOUT_MS.tag, 30000) // Time out since opening the Pad. 0 by default, must set!
                putBoolean(PinParams.RANDOM_KEYBOARD.tag, false) // true by default.

                // 2. Optional
                putBoolean(PinParams.ONLINE_PIN.tag, true) // True by default. This is Online PIN for PICC
                putBoolean(PinParams.BYPASS.tag, true) // False by default. Support 0 PIN or not. Same effect as SUPPORT_PIN_LEN including "0
                putBoolean(PinParams.SOUND.tag, false) // False by defaultSound will be turned on when using the PinPad (lasting effect);
                putBoolean(PinParams.FULL_SCREEN.tag, true) // True by default. Won't have Cancel button when half screen
//                putBoolean(PinParams.RANDOM_KEYBOARD_LOCATION.value, false) // false by default. The keypad moving up & down for security reason
//                putBoolean(PinParams.INPUT_BY_SECURITY_PIN_PAD.value, false) // false by default. Set true to display the Amount.
//                putString(PinParams.INFO_LOCATION.value, "CENTER") // CENTER by default. Can change to LEFT or RIGHT
            }
            if (mPinpadManager.DukptGetKsn(Dukpt.PIN.index, ByteArray(10)) == 0x00) {
                Log.e(TAG, "onRequestPinEntry: DUKPT Pinpad was called")
                cvmAddition.append("DUKPT Pinpad was called\n\n")
                pinpadBundle.putInt(PinParams.PIN_KEY_NO.tag, Dukpt.PIN.index) // Must set, will call onError otherwise
                mPinpadManager.GetDukptPinBlock(pinpadBundle, mPinInputListener)
            } else {
                Log.e(TAG, "onRequestPinEntry: MK/SK Pinpad was called")
                cvmAddition.append("MK/SK Pinpad was called\n\n")
                pinpadBundle.putInt(PinParams.PIN_KEY_NO.tag, 99) // The keySlot of the PIN_KEY to use for encryption. Must Set since onlinePin must be encrypted!
                mPinpadManager.getPinBlockEx(pinpadBundle, mPinInputListener)
            }
        }

        override fun onNFCTransResult(result: ContantPara.NfcTransResult?) {
            Log.e(TAG, "onNFCTransResult: result=${result}")
            when (result) {
                ContantPara.NfcTransResult.ONLINE_APPROVAL,
                ContantPara.NfcTransResult.OFFLINE_APPROVAL -> {
                    mySoundTool.playSound(SOUND_TYPE_SUCCESS)
                }
                ContantPara.NfcTransResult.TERMINATE,
                ContantPara.NfcTransResult.CARD_REMOVED -> {
                    mySoundTool.playSound(SOUND_TYPE_ERROR)
                    
                }
                ContantPara.NfcTransResult.RETRY -> {
                    mEmvKernelManager.abortKernel()
                    onStartEmvButtonClicked(ContantPara.CheckCardMode.SWIPE_OR_INSERT_OR_TAP)
                    return
                }
                else -> null
            }

            this@HomeFragment.result.apply {
                append("TVR: ${mEmvKernelManager.getValByTag(0x95)}\n")
                append("${parseTVRHits(mEmvKernelManager.getValByTag(0x95))}\n\n")
                append("<==========Transaction Ends==========>")
            }

            this@HomeFragment.result.insert(0, "<============$result============>\n\n")
            requireActivity().runOnUiThread {
                btnStartEmv.isEnabled = true
                btnStopEmv.isEnabled = false
                hideBlockingUi()
                isKernelRunning = false
                tvResult.text = this@HomeFragment.result
                Toast.makeText(requireContext(), result.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        override fun onNFCErrorInfor(
            errorCode: ContantPara.NfcErrMessageID?,
            strErrInfo: String?
        ) {
            Log.e(TAG, "onNFCErrorInfor: ")

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
                    if (!bypassPinPadFlag) {
                        bypassPinPadFlag = true
                        mEmvKernelManager.ProcOnlinePinAgain() // Give one more chance to enter
                    } else {
                        mEmvKernelManager.bypassPinEntry() // PIN entry required, PIN pad present, but PIN was not entered
                    }
                } else {
                    Log.e(TAG, "onConfirm: pinBlock=${String(pinBlock)}")
                    pinBlockCache = pinBlock
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
                    if (!bypassPinPadFlag) {
                        bypassPinPadFlag = true
                        mEmvKernelManager.ProcOnlinePinAgain() // Give one more chance to enter
                    } else {
                        mEmvKernelManager.bypassPinEntry() // PIN entry required, PIN pad present, but PIN was not entered
                    }
                } else {
                    Log.e(TAG, "onConfirm_dukpt: pinBlock=${String(pinBlock)}")
                    Log.e(TAG, "onConfirm_dukpt: KSN=${BytesUtil.bytes2HexString(ksn)}")
                    pinBlockCache = pinBlock
                    ksnCache = ksn
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
                requireActivity().runOnUiThread { Toast.makeText(requireContext(), "PIN_KEY not exists", Toast.LENGTH_SHORT).show() }
            }
            mEmvKernelManager.cancelPinEntry()
        }
    }



    // <---------------UI helper methods---------------> //

    private fun uiRefreshOnTermParamsUpdated() {
        tvResult.text = buildString {
            append("<=======Terminal Params updated=======>\n\n")
//            append("IFD_SN_ASCII: 12345678\n")
//            append("TID_ASCII: 87654321\n")
//            append("Merchant_Category: 7011(Hotel)\n")
//            append("Currency(Fixed): 0978(EURO)\n")
            append("Terminal_Type:${sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE)}\n")
            append("Country: ${sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_TERMINAL_COUNTRY_CODE)}\n")
            append("Transaction_Currency_Exp(Fixed): 02\n")
            append("Terminal_Capabilities: ${sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES)}\n")
            append("Add_Terminal_Capabilities: 6000F0A001(Fixed)\n")
            append("ThresholdRandomSwitch: ${thresholdRandomSwitchMap[sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH) ?: DEFAULT_THRESHOLD_RANDOM_SWITCH]}\n")
            append("Target Percentage(Fixed): 50\n")
            append("Max Target Percentage(Fixed): 99")
        }
    }

    private fun uiRefreshOnCapkReloaded() {
        tvResult.text = buildString {
            append("<==========CAPKs loaded==========>\n\n")
            append("UPI_CAPK_INDEX:\n")
            append("[04, 08, 09, 0A, 0B]\n\n")
            append("VISA_CAPK_INDEX:\n")
            append("[08, 09, 53, 57, 92, 94, 96]\n\n")
            append("MasterCard_CAPK_INDEX:\n")
            append("[04, 05, 06, EF, F1, F3, F8, FA, FE]\n\n")
            append("<===========END Loading============>")
        }
    }

    private fun showBlockingUi(msg: String = "Processing...") {
        tvOverlayMsg.text = msg
        blockingOverlay.visibility = View.VISIBLE
        backCallback.isEnabled = true
    }

    private fun hideBlockingUi() {
        blockingOverlay.visibility = View.GONE
        backCallback.isEnabled = false
    }
}







