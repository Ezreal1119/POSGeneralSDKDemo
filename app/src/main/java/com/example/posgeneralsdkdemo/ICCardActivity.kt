package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
//  -> TRM(Terminal Risk Management: Analyze Risk to set TVR, e.g. > Floor Limit or Frequent Transaction)
//  -> CVM(Cardholder Verification Method): No_CVM/Signature; offlinePIN(plain/enciphered); (if onlinePIN, will be done during online transaction)
//  -> TAA(Make Terminal's Final Decision according to rules(TAC/IAC) & results(TVR))
//  -> GAC-1(TC/AAC/ARQC)
//      -> if ARQC(Online Authorization): IS08583(ARQC&Other_PINBlock_MAC) [CDA & onlinePIN will also be done during this Stage]
//      -> GAC-2(TC/AAC)
private const val TAG = "Patrick_ICCardActivity"
class ICCardActivity : AppCompatActivity() {

    private val mICCardReaderManager = InsertCardHandlerImpl.getInstance()

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val etApdu by lazy { findViewById<EditText>(R.id.etApdu) }
    private val btnPowerOn by lazy { findViewById<Button>(R.id.btnPowerOn) }
    private val btnPowerOff by lazy { findViewById<Button>(R.id.btnPowerOff) }
    private val btnCheckCardIn by lazy { findViewById<Button>(R.id.btnCheckCardIn) }
    private val btnSendApdu by lazy { findViewById<Button>(R.id.btnSendApdu) }
    private val btnSle4442 by lazy { findViewById<Button>(R.id.btnSle4442) }
    private val spCardType by lazy { findViewById<Spinner>(R.id.spCardType) }
    private val spApdu by lazy { findViewById<Spinner>(R.id.spApdu) }

    private val cardTypeArray = arrayOf(CardType.ICCard, CardType.PSAM_1, CardType.PSAM_2)
    private val apduCommandArray = arrayOf(
        ApduCommand.SELECT_PSE,
        ApduCommand.READ_RECORD_SFI_1_R_1,
        ApduCommand.SELECT_AID,
        ApduCommand.GPO,
        ApduCommand.APDU_IN_BOX,
        ApduCommand.GET_CHALLENGE)

    private var selectAidDynamic = "-"
    private var gpoDynamic = "-"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iccard)

        btnPowerOn.setOnClickListener { onPowerOnButtonClicked() }
        btnPowerOff.setOnClickListener { onPowerOffButtonClicked() }
        btnCheckCardIn.setOnClickListener { onCheckCardInButtonClicked() }
        btnSendApdu.setOnClickListener { onSendApduButtonClicked() }
        btnSle4442.setOnClickListener { startActivity(Intent(this, Sle4442Activity::class.java)) }

        spCardType.adapter = ArrayAdapter(this, simple_spinner_item, cardTypeArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spApdu.adapter = ArrayAdapter(this, simple_spinner_item, apduCommandArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onStart() {
        super.onStart()
        uiRefreshOnPowerOff()
    }

    override fun onStop() {
        super.onStop()
        if (btnPowerOff.isEnabled) {
            mICCardReaderManager.powerDown(CardType.ICCard.slot)
            selectAidDynamic = "-"
            gpoDynamic = "-"
        }
    }

    private fun onPowerOnButtonClicked() {
        if (!mICCardReaderManager.isCardIn) {
            Toast.makeText(this, "Please insert the ICCard first", Toast.LENGTH_SHORT).show()
            return
        }
        val atrBuffer = ByteArray(64)
        val selectedCardType = spCardType.selectedItem as CardType
        runCatching {
            val outputLen = mICCardReaderManager.powerUp(selectedCardType.slot, atrBuffer)
            if (outputLen <= 0) throw Exception("Power on failed")
            return@runCatching outputLen
        }.onSuccess { outputLen ->
            uiRefreshOnPowerOn()
            val data = atrBuffer.copyOf(outputLen)
            tvResult.text = buildString {
                append("Power on successfully! ATR: \n")
                append(BytesUtil.bytes2HexString(data))
                append("\n\n")
                append("Note: if any ATR(Answer to Reset) returns, then means ICC is powered on successfully.")
            }
            spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.SELECT_PSE))
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onPowerOffButtonClicked() {
        val selectedCardType = spCardType.selectedItem as CardType
        runCatching {
            val ret = mICCardReaderManager.powerDown(selectedCardType.slot)
            if (!ret) throw Exception("Power on failed")
        }.onSuccess {
            uiRefreshOnPowerOff()
            DebugUtil.logAndToast(this, TAG, "Power on successfully")
            tvResult.text = ""
            selectAidDynamic = "-"
            gpoDynamic = "-"
            etApdu.setText("")
        }.onFailure {
            tvResult.text = it.message
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
            tvResult.text = buildString {
                append("ICCard status: \n$isCardIn\n\n")
                append("PSAM_1 status: \n$isPsam1In\n\n")
                append("PSAM_2 status: \n$isPsam2In")
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onSendApduButtonClicked() {
        val apduStr = if (spApdu.selectedItem as ApduCommand == ApduCommand.APDU_IN_BOX) {
            etApdu.text.toString()
        } else if (spApdu.selectedItem as ApduCommand == ApduCommand.SELECT_AID) {
            selectAidDynamic
        } else if (spApdu.selectedItem as ApduCommand == ApduCommand.GPO) {
            gpoDynamic
        } else {
            (spApdu.selectedItem as ApduCommand).apduStr
        }
        val apdu = BytesUtil.hexString2Bytes(apduStr)
        runCatching {
            val startTime = System.currentTimeMillis()
            val rspData = mICCardReaderManager.exchangeApdu(CardType.ICCard.slot, apdu)
            val endTime = System.currentTimeMillis()
            if (rspData == null) throw Exception("No APDU received. \nAPDU_sent = $apduStr")
            Triple(startTime, rspData, endTime)
        }.onSuccess { (startTime, rspData, endTime) ->
            tvResult.text = buildString {
                append("APDU sent: (${(spApdu.selectedItem as ApduCommand)})\n$apduStr \n\n")
                append("APDU received: \n${BytesUtil.bytes2HexString(rspData)}\n\n")
                append("Duration: ${endTime - startTime}ms\n\n")
            }
            when (apduStr) {
                ApduCommand.SELECT_PSE.apduStr -> {
                    spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.READ_RECORD_SFI_1_R_1))
                    tvResult.apply {
                        append("Note: \n")
                        append(" - \"6F\": Prefix for SELECT_resp\n")
                        append(" - \"84\": Name of the File Selected(PSE)\n")
                        append(" - \"A5\": Prefix for File Details\n")
                        append(" - \"88\": SFI(Short File Identifier) e.g. 880101")
                    }
                }
                ApduCommand.READ_RECORD_SFI_1_R_1.apduStr -> {
                    spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.SELECT_AID))
                    etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_1.apduStr)
                    selectAidDynamic = buildString {
                        append("00A40400")
                        append(extractTlvLenHex(BytesUtil.bytes2HexString(rspData), "4F"))
                        append(extractTlvValue(BytesUtil.bytes2HexString(rspData), "4F"))
                        append("00")
                    }
                    tvResult.apply {
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
                ApduCommand.READ_RECORD_SFI_2_R_1.apduStr -> etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_2.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_2.apduStr -> etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_3.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_3.apduStr -> etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_4.apduStr)
                ApduCommand.READ_RECORD_SFI_2_R_4.apduStr -> etApdu.setText(ApduCommand.READ_RECORD_SFI_2_R_5.apduStr)
                selectAidDynamic -> {
                    spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.GPO))
                    val pdol = extractTlvValue(BytesUtil.bytes2HexString(rspData), "9F38") ?: ""
                    gpoDynamic = buildString {
                        append("80A800000C83")
                        append(intTo1ByteHex(pdolTotalBytes(pdol)))
                        append(randomNumberString(pdolTotalBytes(pdol)))
                        append("00")
                    }
                    tvResult.apply {
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
                    spApdu.setSelection(apduCommandArray.indexOf(ApduCommand.READ_RECORD_SFI_1_R_1))
                    tvResult.apply {
                        append("Note: \n")
                        append(" - \"80\": Prefix for GPO_resp\n")
                        append(" - AIP(4Bytes) + AFL(Rest)\n")
                    }
                }
            }
        }.onFailure {
            tvResult.text = it.message
            it.printStackTrace()
        }
    }

    // <-----------------------------Helper methods-----------------------------> //

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
        btnPowerOn.isEnabled = false
        btnPowerOff.isEnabled = true
        btnSendApdu.isEnabled = true
    }

    private fun uiRefreshOnPowerOff() {
        btnPowerOn.isEnabled = true
        btnPowerOff.isEnabled = false
        btnSendApdu.isEnabled = false
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

  3. GPO(Get Procession Options - "80A8"): Send Data to Card based on PDOL, and get AIP & AFL from Card
      - GPO: "80A8"(GPO) + "83"(PDOL Template Prefix; format is consistent with "9F38" received) + "00"
      - "80": GPO response template prefix
        - AIP(Application Interchange Profile - 2 Bytes) + AFL(Application File Locator - the rest Bytes)

Response Status Code:
  - "9000"(Suffix): Success
  - "6A82"(Suffix): Not Found
  - "6700"(Suffix): Lc Not Correct
  - "6D00"(Suffix): Instruction not valid

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