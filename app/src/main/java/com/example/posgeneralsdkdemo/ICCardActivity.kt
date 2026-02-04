package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.databinding.ActivityIccardBinding
import com.example.posgeneralsdkdemo.iccard.Sle4442Activity
import com.example.posgeneralsdkdemo.utils.DebugUtil
import com.urovo.sdk.insertcard.InsertCardHandlerImpl
import com.urovo.sdk.utils.BytesUtil
import kotlin.random.Random

// ICC: SELECT PSE(6F 88-SFI)                           (PICC: SELECT PPSE -> SELECT AID -> GPO(PDOL) -> READ RECORD(APP DATA using AFL))
//  -> READ RECORD AID_LIST(70 4F-AID)
//  -> SELECT AID(6F 9F38-PDOL)
//  -> GPO(80 AIP&AFL)
//  -> READ RECORD APP_DATA()
//  -> ODA(SDA/DDA) (if CDA, will be done during GAC-1)
//  -> PR(Processing Restriction: check rules to set TVR, e.g. Card expired or not)
//  -> CVM(Cardholder Verification Method): No_CVM/Signature; offlinePIN(plain/enciphered); (if onlinePIN, will be done during online transaction)
//  -> TRM(Terminal Risk Management: Analyze Risk to set TVR, e.g. > Floor Limit or Frequent Transaction)
//  -> TAA(Make Terminal's Final Decision according to rules(TAC/IAC) & results(TVR))
//  -> GAC-1(TC/AAC/ARQC)
//      -> if ARQC(Online Authorization): IS08583(ARQC&Other_PINBlock_MAC) [CDA & onlinePIN will also be done during this Stage]
//      -> GAC-2(TC/AAC)
private const val TAG = "Patrick_ICCardActivity"
private val cardTypeArray = arrayOf(CardType.ICCard, CardType.PSAM_1, CardType.PSAM_2)
private val apduCommandArray = arrayOf(
    ApduCommand.SELECT_PSE,
    ApduCommand.READ_RECORD_SFI_1_R_1,
    ApduCommand.SELECT_AID,
    ApduCommand.GPO,
    ApduCommand.APDU_IN_BOX,
    ApduCommand.GET_CHALLENGE)
class ICCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIccardBinding

    private val mICCardReaderManager = InsertCardHandlerImpl.getInstance()

    private var selectAidDynamic = "-"
    private var gpoDynamic = "-"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIccardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartSearch.setOnClickListener { onStartSearchButtonClicked() }
        binding.btnStopSearch.setOnClickListener { onStopSearchButtonClicked() }
        binding.btnPowerOn.setOnClickListener { onPowerOnButtonClicked() }
        binding.btnPowerOff.setOnClickListener { onPowerOffButtonClicked() }
        binding.btnCheckCardIn.setOnClickListener { onCheckCardInButtonClicked() }
        binding.btnSendApdu.setOnClickListener { onSendApduButtonClicked() }
        binding.btnSle4442.setOnClickListener { startActivity(Intent(this, Sle4442Activity::class.java)) }

        binding.spCardType.adapter = ArrayAdapter(this, simple_spinner_item, cardTypeArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        binding.spApdu.adapter = ArrayAdapter(this, simple_spinner_item, apduCommandArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onStart() {
        super.onStart()
        uiRefreshOnPowerOff()
        binding.tvResult.text = buildString {
            append("Three Key Systems in ICCard Transaction:\n\n")
            append(" - ODA: ICCPrivateKey / ICCCert(PK) / IssuerCert(PK) / CA\n")
            append(" - ARQC(CDOL1)/ARPC(8A/89): ICC Master Key / ICC Session Key [Issuer Master Key]\n")
            append(" - PIN encryption(Terminal): Dukpt / PIN_KEY")
        }
    }

    override fun onStop() {
        super.onStop()
        if (binding.btnPowerOff.isEnabled) {
            mICCardReaderManager.powerDown(CardType.ICCard.slot)
            selectAidDynamic = "-"
            gpoDynamic = "-"
        }
    }


    private fun onStartSearchButtonClicked() {
        binding.btnStartSearch.isEnabled = false
        binding.btnPowerOn.isEnabled = false
        binding.btnStopSearch.isEnabled = true
        Toast.makeText(this, "Please insert ICCard", Toast.LENGTH_SHORT).show()
        startDetectOnce (object : CardDetectListener {
            override fun onDetected() {
                binding.btnStopSearch.isEnabled = false
                Toast.makeText(this@ICCardActivity, "Card detected", Toast.LENGTH_SHORT).show()
                val atrBuffer = ByteArray(64)
                val selectedCardType = binding.spCardType.selectedItem as CardType
                runCatching {
                    val outputLen = mICCardReaderManager.powerUp(selectedCardType.slot, atrBuffer)
                    if (outputLen <= 0) throw Exception("Power on failed")
                    return@runCatching outputLen
                }.onSuccess { outputLen ->
                    uiRefreshOnPowerOn()
                    val data = atrBuffer.copyOf(outputLen)
                    binding.tvResult.text = buildString {
                        append("Power on successfully! ATR: \n")
                        append(BytesUtil.bytes2HexString(data))
                        append("\n\n")
                        append("Note: if any ATR(Answer to Reset) returns, then means ICC is powered on successfully.")
                    }
                    binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.SELECT_PSE))
                }.onFailure {
                    binding.tvResult.text = it.message
                    it.printStackTrace()
                }
            }

            override fun onTimeout() {
                Toast.makeText(this@ICCardActivity, "Timeout=20s!", Toast.LENGTH_SHORT).show()
                uiRefreshOnPowerOff()
            }
        })
    }

    private fun onStopSearchButtonClicked() {
        runCatching {
            detectThread.interrupt()
        }.onSuccess {
            uiRefreshOnPowerOff()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onPowerOnButtonClicked() {
        if (!mICCardReaderManager.isCardIn) {
            Toast.makeText(this, "Please insert the ICCard first", Toast.LENGTH_SHORT).show()
            return
        }
        val atrBuffer = ByteArray(64)
        val selectedCardType = binding.spCardType.selectedItem as CardType
        runCatching {
            val outputLen = mICCardReaderManager.powerUp(selectedCardType.slot, atrBuffer)
            if (outputLen <= 0) throw Exception("Power on failed")
            return@runCatching outputLen
        }.onSuccess { outputLen ->
            uiRefreshOnPowerOn()
            val data = atrBuffer.copyOf(outputLen)
            binding.tvResult.text = buildString {
                append("Power on successfully! ATR: \n")
                append(BytesUtil.bytes2HexString(data))
                append("\n\n")
                append("Note: if any ATR(Answer to Reset) returns, then means ICC is powered on successfully.")
            }
            binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.SELECT_PSE))
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPowerOffButtonClicked() {
        if (!mICCardReaderManager.isCardIn) {
            Toast.makeText(this, "Card has been removed!", Toast.LENGTH_SHORT).show()
            uiRefreshOnPowerOff()
            return
        }
        val selectedCardType = binding.spCardType.selectedItem as CardType
        runCatching {
            val ret = mICCardReaderManager.powerDown(selectedCardType.slot)
            if (!ret) throw Exception("Power off failed")
        }.onSuccess {
            uiRefreshOnPowerOff()
            DebugUtil.logAndToast(this, TAG, "Power off successfully")
            binding.tvResult.text = ""
            selectAidDynamic = "-"
            gpoDynamic = "-"
            binding.etApdu.setText("")
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSendApduButtonClicked() {
        if (!mICCardReaderManager.isCardIn) {
            Toast.makeText(this, "Card has been removed!", Toast.LENGTH_SHORT).show()
            uiRefreshOnPowerOff()
            return
        }
        val apduStr = if (binding.spApdu.selectedItem as ApduCommand == ApduCommand.APDU_IN_BOX) {
            binding.etApdu.text.toString()
        } else if (binding.spApdu.selectedItem as ApduCommand == ApduCommand.SELECT_AID) {
            selectAidDynamic
        } else if (binding.spApdu.selectedItem as ApduCommand == ApduCommand.GPO) {
            gpoDynamic
        } else {
            (binding.spApdu.selectedItem as ApduCommand).apduStr
        }
        val apdu = BytesUtil.hexString2Bytes(apduStr)
        runCatching {
            val startTime = System.currentTimeMillis()
            val rspData = mICCardReaderManager.exchangeApdu(CardType.ICCard.slot, apdu)
            val endTime = System.currentTimeMillis()
            if (rspData == null) throw Exception("No APDU received. \nAPDU_sent = $apduStr")
            Triple(startTime, rspData, endTime)
        }.onSuccess { (startTime, rspData, endTime) ->
            binding.tvResult.text = buildString {
                append("APDU sent: (${(binding.spApdu.selectedItem as ApduCommand)})\n$apduStr \n\n")
                append("APDU received: \n${BytesUtil.bytes2HexString(rspData)}\n\n")
                append("Duration: ${endTime - startTime}ms\n\n")
            }
            when (apduStr) {
                ApduCommand.SELECT_PSE.apduStr -> {
                    binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.READ_RECORD_SFI_1_R_1))
                    binding.tvResult.apply {
                        append("Note: \n")
                        append(" - \"6F\": Prefix for SELECT_resp\n")
                        append(" - \"84\": Name of the File Selected(PSE)\n")
                        append(" - \"A5\": Prefix for File Details\n")
                        append(" - \"88\": SFI(Short File Identifier) e.g. 880101")
                    }
                }
                ApduCommand.READ_RECORD_SFI_1_R_1.apduStr -> {
                    binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.SELECT_AID))
                    binding.etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_1.apduStr)
                    selectAidDynamic = buildString {
                        append("00A40400")
                        append(extractTlvLenHex(BytesUtil.bytes2HexString(rspData), "4F"))
                        append(extractTlvValue(BytesUtil.bytes2HexString(rspData), "4F"))
                        append("00")
                    }
                    binding.tvResult.apply {
                        append("Note: \n")
                        append(" - \"70\": Prefix for READ_RECORD_resp\n")
                        append("If READ AID_LIST: [\"61\"(App Template) -> \"4F\"(AID) + \"50\"(APP_Name_ASCII) + \"87\"(APP_Priority)]\n")
                        append("If READ AFL: \n")
                        append(" - \"5A\": App PAN\n")
                        append(" - \"5F24\": App Expiry Date\n")
                        append(" - \"5F25\": App Effective Date\n")
                        append(" - \"5F28\": App Issuer Country Code\n")
                        append(" - \"90\"/\"92\"/\"9F32\": App Issuer Certificate / App Issuer Certificate Remainder / App Issuer PublicKey\n")
                        append(" - \"9F46\"/\"9F47\"/\"9F48\": App ICC Certificate / App ICC PublicKey / App ICC Certificate Remainder\n")
                        append(" - \"9F0D\"/\"9F0E\"/\"9F0F\": IAC_Default / IAC_Denial / IAC_Online\n")
                    }
                }
                ApduCommand.READ_RECORD_SFI_2_R_1.apduStr -> binding.etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_2.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_2.apduStr -> binding.etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_3.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_3.apduStr -> binding.etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_4.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_4.apduStr -> binding.etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_5.apduStr)
                selectAidDynamic -> {
                    binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.GPO))
                    val pdol = extractTlvValue(BytesUtil.bytes2HexString(rspData), "9F38") ?: ""
                    gpoDynamic = buildString {
                        append("80A800000C83")
                        append(intTo1ByteHex(pdolTotalBytes(pdol)))
                        append(randomNumberString(pdolTotalBytes(pdol)))
                        append("00")
                    }
                    binding.tvResult.apply {
                        append("Note: \n")
                        append(" - \"6F\": Prefix for SELECT_resp\n")
                        append(" - \"84\": Name of the File Selected(AID)\n")
                        append(" - \"A5\": Prefix for File Details\n")
                        append(" - \"50\"/\"87\": APP_Name_ASCII / APP_Priority\n")
                        append(" - \"9F38\": PDOL(Processing Data Object List - GPO)\n")
                        append("   - \"9F02\": Amount\n")
                        append("   - \"9A\": Transaction Date\n")
                        append("   - \"9C\": Transaction Type(00 is Purchase)\n")
                    }
                }
                gpoDynamic -> {
                    binding.spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.READ_RECORD_SFI_1_R_1))
                    binding.tvResult.apply {
                        append("Note: \n")
                        append(" - \"80\": Prefix for GPO_resp\n")
                        append(" - AIP(4Bytes) + AFL(Rest)\n")
                    }
                }
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCheckCardInButtonClicked() {
        runCatching {
            val isCardIn = mICCardReaderManager.isCardIn
            val isPsam1In = mICCardReaderManager.isPSAMCardExists(CardType.PSAM_1.slot)
            val isPsam2In = mICCardReaderManager.isPSAMCardExists(CardType.PSAM_2.slot)
            Triple(isCardIn, isPsam1In, isPsam2In)
        }.onSuccess { (isCardIn, isPsam1In, isPsam2In) ->
            binding.tvResult.text = buildString {
                append("ICCard status: \n$isCardIn\n\n")
                append("PSAM_1 status: \n$isPsam1In\n\n")
                append("PSAM_2 status: \n$isPsam2In")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    // <-----------------------------Helper methods-----------------------------> //

    private interface CardDetectListener {
        fun onDetected()
        fun onTimeout()
    }

    private lateinit var detectThread: Thread

    private fun startDetectOnce(listener: CardDetectListener) {
        detectThread = Thread {
            Handler(Looper.getMainLooper()).postDelayed({
                listener.onTimeout()
                detectThread.interrupt()
            }, 20000)
            while (true) {
                if (mICCardReaderManager.isCardIn) {
                    runOnUiThread { listener.onDetected() }
                    return@Thread
                }
                runCatching {
                    Thread.sleep(50)
                }.onFailure {
                    return@Thread
                }
            }
        }.apply { start() }
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
    private fun extractTlvValue(hexString: String, tag: String): String? {
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
    private fun extractTlvLenHex(hexString: String, tag: String): String? {
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
    private fun pdolTotalBytes(pdolHex: String): Int {
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
    private fun randomNumberString(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length * 2) {
            sb.append(Random.nextInt(0, 10))
        }
        return sb.toString()
    }

    // <---------------UI helper methods--------------->
    private fun uiRefreshOnPowerOn() {
        binding.btnPowerOn.isEnabled = false
        binding.btnPowerOff.isEnabled = true
        binding.btnSendApdu.isEnabled = true
        binding.btnStartSearch.isEnabled = false
    }

    private fun uiRefreshOnPowerOff() {
        binding.btnStartSearch.isEnabled = true
        binding.btnPowerOn.isEnabled = true
        binding.btnPowerOff.isEnabled = false
        binding.btnSendApdu.isEnabled = false
        binding.btnStopSearch.isEnabled = false
    }
}

enum class CardType(val slot: Byte) {
    ICCard(0),
    PSAM_1(1),
    PSAM_2(2)
}

enum class ApduCommand(val apduStr: String) {
    SELECT_PSE("00A404000E315041592E5359532E444446303100"),
    SELECT_PPSE("00A404000E325041592E5359532E444446303100"),
    // "00A40400"("00A4040C"): (SELECT_prefix + Select by DF Name)
    // "0E": Lc = 14 Bytes -> PSE/PPSE
    // "315041592E5359532E4444463031": SELECT_PSE(1PAY.SYS.DDF01); If "325041592E5359532E4444463031", then SELECT_PPSE(2PAY.SYS.DDF01)
    READ_RECORD_SFI_1_R_1("00B2010C00"),
    READ_RECORD_SFI_2_R_1("00B2011400"),
    READ_RECORD_SFI_2_R_2("00B2021400"),
    READ_RECORD_SFI_2_R_3("00B2031400"),
    READ_RECORD_SFI_2_R_4("00B2041400"),
    READ_RECORD_SFI_2_R_5("00B2051400"),
    SELECT_AID("<SELECT_AID>"),
    GPO("<GPO>"),
    // "07": Lc = 7 Bytes -> AID(RID + AID_suffix)
    // "A000000003101000": Visa Credit
    GET_CHALLENGE("0084000004"),
    APDU_IN_BOX("")
}


// ICC: SELECT PSE(6F 88-SFI)                           (PICC: SELECT PPSE -> SELECT AID -> GPO(PDOL) -> READ RECORD(APP DATA using AFL))
//  -> READ RECORD AID_LIST(70 4F-AID)
//  -> SELECT AID(6F 9F38-PDOL)
//  -> GPO(80 AIP&AFL)
//  -> READ RECORD APP_DATA()
//  -> ODA(SDA/DDA) (if CDA, will be done during GAC-1)
//  -> PR(Processing Restriction: check rules to set TVR, e.g. Card expired or not)
//  -> TRM(Terminal Risk Management: Analyze Risk to set TVR, e.g. > Floor Limit or Frequent Transaction)
//  -> CVM(Cardholder Verification Method): No_CVM/Signature; offlinePIN(plain/enciphered); (if onlinePIN, will be done during online transaction)
//  -> TAA(Make Terminal's Final Decision according to rules(TAC/IAC) & results(TVR))
//  -> GAC-1(TC/AAC/ARQC)
//      -> if ARQC(Online Authorization): IS08583(ARQC&Other_PINBlock_MAC) [CDA & onlinePIN will also be done during this Stage]
//        -> ARPC(91) = ARC(8A - Approve(3030); Decline(3035); InsufficientBalance(3531)) + AC(89 - SN of ARPC)
//      -> GAC-2(TC/AAC)

// SDA: Kernel uses IssuerPK(verified using CAPK) to verify the SSAD("93"). Can be done right after GPO
// DDA: Kernel uses IssuerPK(verified using CAPK) to verify ICC_Cert -> Send Challenge(DDOL) and verify SDAD("9F4B" only) using ICC_PK. Can be done right after GPO
// CDA: Kernel uses IssuerPK(verified using CAPK) to verify ICC_Cert -> Send Challenge(DDOL and verify SDAD("9F4B" & AC) using ICC_PK. Can only be done after GAC-1

/*
    Understanding AC(Application Cryptogram - 9F26):
        -> Transaction_Data encrypted using Calculated_SessionKey(Prepare for ARQC)
        -> Make the final decision on how to process the transaction
    - "80AE80": TC(Transaction Certificate) means Offline approved
    - "80AE40": AAC(Application Authentication Cryptogram) means Declined
    - "80AE00": ARQC(Authorization Request Cryptogram) means Online authorization needed -> ISO8583(DE55_PINBlock_MAC)(ARPC) -> GAC-2(TC if Issuer approved) / AAC if Issuer declined)
 */

/*
    Understanding ISO8583:
        -> DE55(ARQC_9F26+Other_EMV_TLV)
        -> PINBlock(PIN + PAN)
        -> MAC(CheckSum)
 */

/*
    Understanding PDOL / DDOL: Card tells how Terminal should format the Data and send to Card
        - PDOL(Processing Data Object List): Used to format GPO
        - DDOL(Dynamic Data Object List): Used to format DDA/CDA Challenge
 */

/*
Understanding APDU_resp(TLV from Card)
  1. SELECT Command: "00A40400"("00A4040C")
    SELECT PSE:
      - SELECT PSE: "00A40400"("00A4040C") + "0E" + "315041592E5359532E4444463031"(1PAY.SYS.DDF01) + "00"
      - SELECT PPSE: "00A40400"("00A4040C") + "0E" + "325041592E5359532E4444463031"(2PAY.SYS.DDF01) + "00"
      - "6F": File Template Prefix indicates Response to SELECT Command, followed by Lc
        - "84": Name of the File. e.g. "315041592E5359532E4444463031"(1PAY.SYS.DDF01), followed by Lc
        - "A5": Extra info Template Prefix, followed by Lc
          a. if ICC(PSE): "88"(SFI - Short File Identifier) tells Terminal where to READ the list of AIDs
          b. if PICC(PPSE): "BF0C"(AID list); followed by one/more ["61"(App Template) -> "4F"(AID) + "50"(APP_Name_ASCII) + "87"(APP_Priority)]
    SELECT AID:
      - SELECT AID: "00A40400"("00A4040C") + "07" + AID + "00"
      - "6F": File Template Prefix indicates Response to SELECT Command, followed by Lc
        - "84": Name of the File. e.g. "A0000000031010"(AID)
        - "A5": Extra info Template Prefix, followed by Lc
          - "50"(APP_Name_ASCII)
          - "87"(APP_Priority)
          - "9F38"(PDOL - Processing Data Object List): to tell Terminal how to format the GPO data packet. e.g. "9F66049F02069A039C019F37049F3501" -> "9F66 04"+"9F02 06"+"9A 03"+"9C 01"+"9F37 04"+"9F35 01"
            - "9F66"(TTQ - Terminal Transaction Qualifiers): e.g. "34204000"
              Understanding TTQ: It's Kernel decides based on the Kernel Policy(logic+Current situation) & Terminal Capabilities
              It's Kernel(Not Terminal) telling the Card which payment options are supported.
            - "9F02"(Amount): e.g. "000000000100" means the amount is 1.00
            - "9A"(Transaction Date): e.g. "240418" means the Transaction Date is 2024-04-18
            - "9C"(Transaction Type): e.g. "00"(Purchase); "01"(Withdrawal); "09"(CashBack); "20"(Refund)
            - "9F37"(Random Number): e.g. "D31ED8BF" used for DDA/CDA and generating Cryptogram
            - "9F35"(Terminal Type)
            - "8C"(CDOL1) / "8D"(CDOL2)
              - "8C"(GAC-1): "9F02"(Amount) / "95"(TVR)
              - "8A"(ARC): 3030; 3035; 3531

  2. READ RECORD: "00B2"(READ RECORD)
    READ RECORD(AID LIST): This only applies to ICC, not PICC
      - READ RECORD(AID LIST): "00B2"(READ RECORD) + P1 + P2 + "00", e.g. "00 B2 01 0C 00" is to read the first Record(SFI=1); "00 B2 01 14 00" is to read the second Record(SFI=2)
      - "70": Record Template Prefix indicates Response to READ RECORD Command, follower by Lc
        a. if ICC reading AID LIST: one/more ["61"(App Template) -> "4F"(AID) + "50"(APP_Name_ASCII) + "87"(APP_Priority)]
        b. if ICC/PICC reading APP info from AFL:
          - "5F24": Application Expiration Date
          - "5F25": Application Effective Date
          - "5F28": Application Issuer Country Code
          - "5A": Application PAN(Primary Account Number) -> Must note: PAN is Application-level data. Different APPs might have difference PANs. But normally, All APPs are using the same PAN which is the one printed on the Surface of the Card.
          - "5F34": Application PAN Sequence Number
          - "9F0D"(IAC_Default) / "9F0E"(IAC_Denial) / "9F0F"(IAC_Online)
          - "90"(Issuer Certificate): Terminal uses CAPK to verify, and extract Public Key from it. Then use for ODA(SDA/DDA/CDA) ["92"(Remainder since "90" might not cover all Certificate Content) / "9F32"(IssuerPK since EMV Issuer Certificate doesn't contain IssuerPK like normal)]
            - SDA(Static Data Authentication): CAPK -> Issuer Cert(PK) -> "93"(SSAD - Signed Static Application Data)
            - DDA(Dynamic Data Authentication): CAPK -> Issuer Cert(PK) -> "9F46"(ICC Cert(PK)) -> "9F4B"(SDAD - Signed Dynamic Application Data using Challenge from Terminal)
            - CDA(Combined Data Authenticaiton): Kernel uses IssuerPK(verified using CAPK) to verify ICC_Cert -> Send Challenge(DDOL and verify SDAD("9F4B" & AC) using ICC_PK. Can only be done after GAC-1
          - "9F46"(ICC Certificate): Terminal uses CAPK to verify it, then extract the ICC public key for DDA/CDA ["9F47" / "9F48"]
          - "8E"(CVM): Threshold_1(4 Bytes) + Threshold_2(4 Bytes) + CVM_RULE_1(2 Bytes) + CVM_RULE_2(2 Bytes) + ...
            - 4203(Online PIN if terminal supports);
            - 0403(Enciphered Offline PIN if terminal supports);
            - 0203(Plain Offline PIN if terminal supports);
            - 1E03(Signature if terminal supports)
            - 1F03(No CVM if terminal supports)
            - e.g., 00000000 00000000 4203 0303



  3. GPO(Get Procession Options - "80A8"): Send Data to Card based on PDOL, and get AIP & AFL from Card
      - GPO: "80A8"(GPO) + "83"(PDOL Template Prefix; format is consistent with "9F38" received) + "00"
      - "80": GPO response template prefix
        - AIP(Application Interchange Profile - 2 Bytes) + AFL(Application File Locator - the rest Bytes)
          - AIP(2 Bytes):
            - First Byte: SDA_Supported - DDA_Supported - CVM_Supported - TRM_Needed - OnlineTransaction_Supported - CDA_Supported - RFU - RFU
            - Second Byte: normally RFU 0x00
              - FC00 means Support all(SDA/DDA/CDA CVM OnlineTransaction TRM_needed)
              - 7C00 means only not support SDA but the rest
Response Status Code:
  - "9000"(Suffix): Success
  - "6A82"(Suffix): Not Found
  - "6700"(Suffix): Lc Not Correct
  - "6D00"(Suffix): Instruction not valid
  - "6985"(Suffix): Condition of use not satisfied

Understand TVR(Terminal Verification Result):
  - It's 5 Bytes. e.g. "04 C0 00 00 00"
  - It's consisting monitoring the whole transaction process, dynamically changed as per the current situation. When trigger, certain bit will be flip from 0 to 1
  - Once some certain condition is met, certain bit will be set to 1 for future decision making.
 */

/*
    Understanding Magnetic Stripe Card Transaction:
        - Terminal read Track_2 from the MagCard(PAN + ServiceCode + ExpirationDate + CVV_3digits)
        - Then ask for PIN and form ISO8583 and send to Issuer for verification (Issuer verifies using CVV & PIN)
        - If approved, then will callback, then end of the transaction
 */

/*
Other notes:
    - Need to finish READ RECORD first before proceeding to ODA/PR/TRM/CVM/TAA
    - READ RECORD(GPO) only collects & saves all the TLVs first from Card, even might not be used. Will extract all the TLVs needed in real time during each stage from the Kernel's RAM
    -

ICC:
1. SELECT PSE: 6F -> 3150(ICC)
2. READ RECORD: 70 -> 4F(AID)/50(ASCII)/87(Priority)
3. SELECT AID: 6F -> 9F38(PDOL)
4. GPO: 80 -> AIP / AFL
5. READ RECORD: 70 -> 5A(57)/5F24/5F25/5F30 - 9F07/9F0D/9F0E/9F0F - 90/9F32/92(8F) - 9F46/9F47/9F48(9F49-DDOL) - 93（SSAD) - 8E(APP_CVM) - 8C(CDOL1: 95/9F02)/8D(CDOL2: 8A)
6. ODA(Check AIP & TERM_CAP(9F33) -> Select Requested CAPK_INDEX(8F) -> Verify the IssuerCert(90) & Extract the IssuerPK): CardAuth
 - Will result in Change of TVR(95) bit if failed
7. PR(Check Trans_Type(9C) & Term_Country(9F35) & AUC(9F07) if support): ProcRestric
 - Mostly check if Trans_Type&Term_Country can pass AUC or not.
 - Wil result in Change of TVR(95) bit if failed
8. CVM(Check ICC_CVM(8E) & TERM_CAP(9F33)): CMVStart
 - Will result in how to proceed CVM(ONLINE_PIN / OFFLINE PIN / SIGNATURE / NO_CVM)
 - Will invoke the PINPAD if needed: SetPinCVR
9.  TRM(Check TFL_LIMIT+Amount(9F1B+9F02) & RandomOnline) : EMVRiskManagement
10. TAA(Finally decide what to do - Based on TVR & TAC & IAC; Check from DENIAL->ONLINE->DEFAULT): TermActAnalyse
 - e.g.: TermActAnalyse(TAC_DENIAL):0; TermActAnalyse(TAC_ONLINE):-1 ["0" means no hit, hits otherwise]
 - Please note: TAA might take place beforehand to avoid unnecessary work
11. GAC-1: GAC -> CID(80/40/00) + ATC(Counter_2_Bytes) + ARQC(8_Bytes_EncryptedFromCDOL1_Using_SK)[if Online]
12. ISO8583(GetF55): pIccData -> 8A(ARC) + 89（AC)
13. GAC-2: GAC -> CID(80/40/00) + ATC(Counter_2_Bytes) + TC/AAC(8_Bytes_EncryptedFromCDOL2_Using_SK)


Three types of CIDs(Cryptogram Information Data): inside GAC-1_Resp
 - 80: ARPC (Request for Online)
 - 40: TC (Approve)
 - 00: AAC (Decline)


Three types of KEY systems in a EMV transaction:
 - DUKPT(Terminal) for PIN
 - ICCKeyPair(ICC) for ODA
 - MK/SK(ICC/Issuer) for ARPC/ARQC: (Issue generate the same SK using the same MK&Trans_Data, then calculate the ARPC based on the Trans_Data to see if it's the same as the original one. Card will do the same to ARPC)


Understanding AUC (Application Usage Control): FF00 means no restriction for the usage of the Card. Used in PR


Three types of ARC(8A - Authorization Request Code) from ISO8583_Resp:
 - 3030: Approval
 - 3035: Decline
 - 3531: Insufficient Balance


DDA whole procedure:
 - Card tells AID(4F) & CAPK_INDEX(8F). Kernel looks for CAPK at that slot and extract the CAPK
 - Kernel verifies the Issuer Certificate(RSA to check Signature), then extract the IssuerPK from the Cert.
 - Kernel verifies the ICC Certificate(HASH_after == HASH-crt ?), then extract the ICCPK from the Cert.
 - Kernel sends APDU(CardAuth) as per DDOL, and verifies the Resp using the ICCPK. (DDOL normally Random Number) (How to verify? To decrypt the Resp using ICCPK, then check if it's the same as the Random Number sent from Terminal to the Card)
 -> If succeeded: DDA_Auth():0
 */

