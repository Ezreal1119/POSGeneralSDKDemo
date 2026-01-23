package com.example.posgeneralsdkdemo.utils

import android.content.SharedPreferences
import com.example.posgeneralsdkdemo.enums.AppTag
import com.example.posgeneralsdkdemo.enums.CapkTag
import com.example.posgeneralsdkdemo.enums.TerminalTag
import com.example.posgeneralsdkdemo.fragments.emv.DEFAULT_COUNTRY_CODE
import com.example.posgeneralsdkdemo.fragments.emv.DEFAULT_TERMINAL_CAPABILITIES
import com.example.posgeneralsdkdemo.fragments.emv.DEFAULT_TERMINAL_TYPE
import com.example.posgeneralsdkdemo.fragments.emv.KEY_TERMINAL_CAPABILITIES
import com.example.posgeneralsdkdemo.fragments.emv.KEY_TERMINAL_COUNTRY_CODE
import com.example.posgeneralsdkdemo.fragments.emv.KEY_TERMINAL_TYPE
import com.urovo.i9000s.api.emv.ContantPara
import com.urovo.i9000s.api.emv.EmvNfcKernelApi
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Hashtable

object EmvUtil {
    fun getCardNo(pan: String, track2: String): String {
        var cardNo = pan // PAN
        if (cardNo.isBlank()) {
            cardNo = track2 // Track 2
            if (cardNo.isBlank()) return ""
            cardNo = cardNo.substring(0, cardNo.uppercase().indexOf("D")) // might also be "=" ?
        }
        val lastChar = cardNo[cardNo.length - 1]
        if (lastChar == 'f' || lastChar == 'F' || lastChar == 'd' || lastChar == 'D') {
            cardNo = cardNo.substring(0, cardNo.length - 1)
        }
        return cardNo
    }


    fun parseCid2String(cid: String): String {
        return when (cid) {
            "80" -> "ARQC"
            "40" -> "TC"
            "00" -> "AAC"
            else -> "Unknown"
        }
    }

    fun parseAppCvmRule3Bytes(hex3Bytes: String): String {
        val clean = hex3Bytes.replace(" ", "").uppercase()
        if (clean.length < 4) return "Unknown - Unknown"

        val b1 = clean.substring(0, 2).toIntOrNull(16) ?: return "Unknown - Unknown"
        val b2 = clean.substring(2, 4).toIntOrNull(16) ?: return "Unknown - Unknown"

        val low4 = b1 and 0x0F  // bit1~bit4 (LSB 4 bits)

        val cvm = when (low4) {
            0x0 -> "CVM Failed"              // 0000
            0x2 ->
                "Enciphered ONLINE PIN"      // 0010
            0x1 -> "Plaintext OFFLINE PIN"   // 0001
            0x4 -> "Enciphered OFFLINE PIN"  // 0100
            0xE -> "Signature"               // 1110
            0xF -> "No CVM"                  // 1111
            else -> "Unknown"
        }

        val cond = when (b2) {
            0x03 -> "If Terminal supports"
            0x00 -> "Always"
            else -> "Unknown"
        }

        return "$cvm - $cond"
    }

    fun parseTerminalType2String(terminalType: String): String {
        return when (terminalType) {
            "22" -> "Online POS"
            "14" -> "Offline POS"
            else -> "Unknown"
        }
    }

    fun parseMerchantCategoryCode2String(merchantCode: String): String {
        return when (merchantCode) {
            "7011" -> "Hotel"
            "5812" -> "Restaurant"
            "5814" -> "Fast Food"
            "5311" -> "Department Stores"
            "5411" -> "Supermarket"
            else -> "Unknown"
        }
    }

    fun parseTransactionType2String(transactionType: String): String {
        return when (transactionType) {
            "00" -> "Purchase"
            "01" -> "Withdrawal"
            "09" -> "CashBack"
            "20" -> "Refund"
            else -> "Unknown"
        }
    }

    fun parseCurrencyCode2String(currencyCode: String): String {
        return when (currencyCode) {
            "0156" -> "CNY" // China Yuan
            "0344" -> "HKD" // Hong Kong Dollar
            "0446" -> "MOP" // Macao Pataca
            "0392" -> "JPY" // Japan Yen
            "0410" -> "KRW" // Korea Won
            "0704" -> "VND" // Vietnam Dong
            "0764" -> "THB" // Thailand Baht
            "0360" -> "IDR" // Indonesia Rupiah
            "0458" -> "MYR" // Malaysia Ringgit
            "0702" -> "SGD" // Singapore Dollar
            "0608" -> "PHP" // Philippines Peso

            "0050" -> "BDT" // Bangladesh Taka
            "0586" -> "PKR" // Pakistan Rupee
            "0356" -> "INR" // India Rupee
            "0949" -> "TRY" // Turkey Lira
            "0818" -> "EGP" // Egypt Pound
            "0784" -> "AED" // UAE Dirham
            "0682" -> "SAR" // Saudi Riyal
            "0400" -> "JOD" // Jordan Dinar
            "0414" -> "KWD" // Kuwait Dinar
            "0634" -> "QAR" // Qatar Riyal

            "0840" -> "USD" // US Dollar
            "0978" -> "EUR" // Euro
            "0826" -> "GBP" // UK Pound
            "0756" -> "CHF" // Swiss Franc
            "0124" -> "CAD" // Canada Dollar
            "0036" -> "AUD" // Australia Dollar
            "0554" -> "NZD" // New Zealand Dollar

            "0986" -> "BRL" // Brazil Real
            "0484" -> "MXN" // Mexico Peso
            "0032" -> "ARS" // Argentina Peso
            "0152" -> "CLP" // Chile Peso
            "0604" -> "PEN" // Peru Sol
            "0170" -> "COP" // Colombia Peso

            "0710" -> "ZAR" // South Africa Rand
            "0566" -> "NGN" // Nigeria Naira
            "0404" -> "KES" // Kenya Shilling
            "0834" -> "TZS" // Tanzania Shilling
            "0800" -> "UGX" // Uganda Shilling
            else -> "Unknown"
        }
    }

    fun parseCountryCode2String(countryCode: String): String {
        return when (countryCode) {
            "0156" -> "China (CN)"
            "0344" -> "Hong Kong (HK)" // Part of China
            "0446" -> "Macau (MO)" // Part of China
            "0158" -> "Taiwan (TW)" // Part of China

            "0392" -> "Japan (JP)"
            "0410" -> "Korea (KR)"
            "0704" -> "Vietnam (VN)"
            "0764" -> "Thailand (TH)"
            "0360" -> "Indonesia (ID)"
            "0458" -> "Malaysia (MY)"
            "0702" -> "Singapore (SG)"
            "0608" -> "Philippines (PH)"

            "0050" -> "Bangladesh (BD)"
            "0586" -> "Pakistan (PK)"
            "0356" -> "India (IN)"
            "0144" -> "Sri Lanka (LK)"
            "0524" -> "Nepal (NP)"

            "0792" -> "Turkey (TR)"
            "0818" -> "Egypt (EG)"
            "0784" -> "United Arab Emirates (AE)"
            "0682" -> "Saudi Arabia (SA)"
            "0400" -> "Jordan (JO)"
            "0414" -> "Kuwait (KW)"
            "0634" -> "Qatar (QA)"
            "0512" -> "Oman (OM)"
            "0048" -> "Bahrain (BH)"

            "0840" -> "United States (US)"
            "0124" -> "Canada (CA)"
            "0484" -> "Mexico (MX)"
            "0076" -> "Brazil (BR)"
            "0032" -> "Argentina (AR)"
            "0152" -> "Chile (CL)"
            "0604" -> "Peru (PE)"
            "0170" -> "Colombia (CO)"

            "0826" -> "United Kingdom (GB)"
            "0250" -> "France (FR)"
            "0276" -> "Germany (DE)"
            "0380" -> "Italy (IT)"
            "0724" -> "Spain (ES)"
            "0620" -> "Portugal (PT)"
            "0056" -> "Belgium (BE)"
            "0528" -> "Netherlands (NL)"
            "0752" -> "Sweden (SE)"
            "0578" -> "Norway (NO)"
            "0208" -> "Denmark (DK)"
            "0246" -> "Finland (FI)"
            "0756" -> "Switzerland (CH)"
            "0040" -> "Austria (AT)"
            "0616" -> "Poland (PL)"
            "0203" -> "Czechia (CZ)"
            "0348" -> "Hungary (HU)"
            "0642" -> "Romania (RO)"
            "0100" -> "Bulgaria (BG)"
            "0300" -> "Greece (GR)"
            "0372" -> "Ireland (IE)"
            "0352" -> "Iceland (IS)"
            "0643" -> "Russia (RU)"
            "0804" -> "Ukraine (UA)"

            "0710" -> "South Africa (ZA)"
            "0566" -> "Nigeria (NG)"
            "0404" -> "Kenya (KE)"
            "0834" -> "Tanzania (TZ)"
            "0800" -> "Uganda (UG)"
            "0288" -> "Ghana (GH)"
            "0504" -> "Morocco (MA)"
            "0788" -> "Tunisia (TN)"
            "0434" -> "Libya (LY)"
            "0729" -> "Sudan (SD)"
            "0231" -> "Ethiopia (ET)"

            else -> "Unknown"
        }
    }


    fun parseTVRHits(tvrHex: String?): String {
        if (tvrHex.isNullOrBlank()) return "TVR is null/blank"

        val hex = tvrHex.replace(" ", "").uppercase()
        if (hex.length != 10) return "Invalid TVR length (need 10 hex chars): $hex"

        val tvr = hexToBytes(hex) // 5 bytes

        val hits = StringBuilder()

        fun hit(byteIndex: Int, b: Byte, mask: Int, label: String, msg: String) {
            if ((b.toInt() and 0xFF and mask) != 0) {
                // bit number: 0x80->8, 0x40->7 ... 0x01->1
                val bitNum = when (mask) {
                    0x80 -> 8; 0x40 -> 7; 0x20 -> 6; 0x10 -> 5
                    0x08 -> 4; 0x04 -> 3; 0x02 -> 2; 0x01 -> 1
                    else -> -1
                }
                hits.append("B${byteIndex}b$bitNum = 1 : $msg\n")
            }
        }

        // Byte1
        hit(1, tvr[0], 0x80, "B1b8", "Offline data authentication was not performed")
        hit(1, tvr[0], 0x40, "B1b7", "SDA failed")
        hit(1, tvr[0], 0x20, "B1b6", "ICC data missing")
        hit(1, tvr[0], 0x10, "B1b5", "Card appears on terminal exception file")
        hit(1, tvr[0], 0x08, "B1b4", "DDA failed")
        hit(1, tvr[0], 0x04, "B1b3", "CDA failed")
        hit(1, tvr[0], 0x02, "B1b2", "SDA was selected")
        hit(1, tvr[0], 0x01, "B1b1", "Reserved For Use")

        // Byte2
        hit(2, tvr[1], 0x80, "B2b8", "ICC and terminal have different application versions")
        hit(2, tvr[1], 0x40, "B2b7", "Expired application")
        hit(2, tvr[1], 0x20, "B2b6", "Application not yet effective")
        hit(2, tvr[1], 0x10, "B2b5", "Requested service not allowed for card product")
        hit(2, tvr[1], 0x08, "B2b4", "New card")
        hit(2, tvr[1], 0x04, "B2b3", "Reserved For Use")
        hit(2, tvr[1], 0x02, "B2b2", "Reserved For Use")
        hit(2, tvr[1], 0x01, "B2b1", "Reserved For Use")

        // Byte3
        hit(3, tvr[2], 0x80, "B3b8", "Cardholder verification was not successful")
        hit(3, tvr[2], 0x40, "B3b7", "Unrecognised CVM")
        hit(3, tvr[2], 0x20, "B3b6", "PIN Try Limit exceeded")
        hit(3, tvr[2], 0x10, "B3b5", "PIN entry required, but PIN pad not present or not working")
        hit(3, tvr[2], 0x08, "B3b4", "PIN entry required, PIN pad present, but PIN was not entered")
        hit(3, tvr[2], 0x04, "B3b3", "Online PIN entered")
        hit(3, tvr[2], 0x02, "B3b2", "Reserved For Use")
        hit(3, tvr[2], 0x01, "B3b1", "Reserved For Use")

        // Byte4
        hit(4, tvr[3], 0x80, "B4b8", "Transaction exceeds floor limit")
        hit(4, tvr[3], 0x40, "B4b7", "Lower consecutive offline limit exceeded")
        hit(4, tvr[3], 0x20, "B4b6", "Upper consecutive offline limit exceeded")
        hit(4, tvr[3], 0x10, "B4b5", "Transaction selected randomly of on-line processing")
        hit(4, tvr[3], 0x08, "B4b4", "Merchant forced transaction on-line")
        hit(4, tvr[3], 0x04, "B4b3", "Reserved For Use")
        hit(4, tvr[3], 0x02, "B4b2", "Reserved For Use")
        hit(4, tvr[3], 0x01, "B4b1", "Reserved For Use")

        // Byte5
        hit(5, tvr[4], 0x80, "B5b8", "Default TDOL used")
        hit(5, tvr[4], 0x40, "B5b7", "Issuer authentication failed")
        hit(5, tvr[4], 0x20, "B5b6", "Script processing failed before final GENERATE AC")
        hit(5, tvr[4], 0x10, "B5b5", "Script processing failed after final GENERATE AC")

        if (hits.isEmpty()) return "No TVR bits set (all zero). TVR=$hex"
        return hits.toString().trimEnd()
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


    fun formatAmount12(amountHexOrNumeric: String, exp: Int): String {
        val digits = amountHexOrNumeric.trim()

        // 000000000100 -> 100
        val cents = digits.trimStart('0').ifEmpty { "0" }

        // cents -> dollars with 2 decimals
        return BigDecimal(cents)
            .divide(BigDecimal("100"))
            .setScale(exp, RoundingMode.DOWN)
            .toPlainString()
    }

    fun hexToBinaryBytes(hex: String): String {
        val clean = hex.replace(" ", "").uppercase()

        require(clean.length % 2 == 0) { "Invalid hex length: ${clean.length}" }
        require(clean.all { it in "0123456789ABCDEF" }) { "Invalid hex string: $hex" }

        return clean.chunked(2)
            .joinToString(" ") { byteHex ->
                val v = byteHex.toInt(16)
                v.toString(2).padStart(8, '0')
            }
    }

    fun parseAip(aipHex: String): String {
        val hex = aipHex.trim().replace(" ", "").uppercase()
        if (hex.length < 2) return "Invalid AIP"

        // AIP byte1 = hex[0..1]
        val b1 = hex.substring(0, 2).toInt(16)

        fun bitIs1(bitIndex: Int): Boolean {
            // bitIndex: b8..b1  -> 8..1
            return (b1 shr (bitIndex - 1) and 0x01) == 1
        }

        val lines = mutableListOf<String>()

        // b8 is RFU -> skip

        // b7
        lines += if (bitIs1(7)) " - [B1b7]: SDA Supported" else " - [B1b7]: SDA Not Supported"
        // b6
        lines += if (bitIs1(6)) " - [B1b6]: DDA Supported" else " - [B1b6]: DDA Not Supported"
        // b5
        lines += if (bitIs1(1)) " - [B1b1]: CDA Supported" else " - [B1b1]: CDA Not Supported"
        lines += if (bitIs1(5)) " - [B1b5]: CVM Supported" else " - [B1b5]: CVM Not Supported"
        // b4
        lines += if (bitIs1(4)) " - [B1b4]: TRM is to be performed" else " - [B1b4]: TRM is not to be performed"
        // b3
        lines += if (bitIs1(3)) " - [B1b3]: Issuer auth(Online) Supported" else " - [B1b3]: Issuer auth(Online) Not Supported"
        // b2
        lines += if (bitIs1(2)) " - [B1b2]: On-device CVM(e.g. Facial) Supported" else " - [B1b2]: On-device CVM(e.g. Facial) Not Supported"
        // b1

        return lines.joinToString(separator = "\n")
    }

    fun analyzeTAAResult(
        tvr: String,
        tacDenial: String,
        iacDenial: String,
        tacOnline: String,
        iacOnline: String,
        tacDefault: String,
        iacDefault: String
    ): String {
        /*
        if (tacDenial hit) OR (iacDenial hit) -> decline
        else if (tacOnline hit) OR (iacOnline hit) -> go online
        else if (tacDefault hit) OR (iacDefault hit) -> default action (often online)
        else -> no TAA hit
         */
        fun hexToBytes(hex: String): ByteArray {
            val clean = hex.trim().replace(" ", "").uppercase()
            return ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }

        fun isHit(tvrBytes: ByteArray, maskBytes: ByteArray): Boolean {
            for (i in tvrBytes.indices) {
                if ((tvrBytes[i].toInt() and 0xFF) and (maskBytes[i].toInt() and 0xFF) != 0) {
                    return true
                }
            }
            return false
        }

        val tvrBytes = hexToBytes(tvr)

        val rules = listOf(
            "TAC_DENIAL" to tacDenial,
            "IAC_DENIAL" to iacDenial,
            "TAC_ONLINE" to tacOnline,
            "IAC_ONLINE" to iacOnline,
            "TAC_DEFAULT" to tacDefault,
            "IAC_DEFAULT" to iacDefault
        )

        val hitList = mutableListOf<String>()
        for ((name, maskHex) in rules) {
            if (isHit(tvrBytes, hexToBytes(maskHex))) {
                hitList.add(name)
            }
        }

        return if (hitList.isEmpty()) "NO_HIT" else hitList.joinToString(", ")
    }

    fun updateTerminalParameters(sharedPreferences: SharedPreferences, mEmvKernelManager: EmvNfcKernelApi) {
        val terminalParameters = buildString {
            append(TerminalTag.IFD_SN_ASCII_8.tag) // 9F1E
            append(TerminalTag.IFD_SN_ASCII_8.len) // 08
            append("3132333435363738") // 12345678
            append(TerminalTag.TERMINAL_IDENTIFICATION_ASCII_8.tag) // 9F1C
            append(TerminalTag.TERMINAL_IDENTIFICATION_ASCII_8.len) // 08
            append("3030303030303030") // 00000000
            append(TerminalTag.TERMINAL_TYPE.tag) // 9F35
            append(TerminalTag.TERMINAL_TYPE.len) // 01
            append(sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE)) // Just a label, no effect. 0x22 means support Online transaction. 0x14 means Offline POS.
            append(TerminalTag.MERCHANT_CATEGORY_CODE.tag) // 9F15
            append(TerminalTag.MERCHANT_CATEGORY_CODE.len) // 02
            append("7011") // 7011 means Hotel
            append(TerminalTag.TRANSACTION_CURRENCY_EXPONENT.tag) // 5F36
            append(TerminalTag.TRANSACTION_CURRENCY_EXPONENT.len) // 01
            append("02") // The Currency Exponent is only 02, e.g., $9.99
            append(TerminalTag.CURRENCY_CODE.tag) // 5F2A
            append(TerminalTag.CURRENCY_CODE.len) // 02
            append("0978") // The currency is EURO
            append(TerminalTag.COUNTRY_CODE.tag) // 9F1A
            append(TerminalTag.COUNTRY_CODE.len) // 02
            append(sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_COUNTRY_CODE)) // The country is Spain
            append(TerminalTag.TERMINAL_CAPABILITIES.tag) // 9F33
            append(TerminalTag.TERMINAL_CAPABILITIES.len) // 03
            append(sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES)) // "E0F8C8" means support all; "E0F0C8" means not support No_CVM
            append(TerminalTag.ADDITIONAL_TERMINAL_CAPABILITIES.tag) // 9F40
            append(TerminalTag.ADDITIONAL_TERMINAL_CAPABILITIES.len) // 05
            append("6000F0A001") // Need to refer to the Kernel Document
        }
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.UNKNOWN, terminalParameters)
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.ICC, terminalParameters)
        mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.PICC, terminalParameters)
    }

    // <-----------------Add CAPK-----------------> //
    // https://www.eftlab.com/knowledge-base/list-of-ca-public-keys


    fun addCapkUpi(mEmvKernelManager: EmvNfcKernelApi) { // UnionPay International
        // UPI_CAPK_INDEX: [04, 08, 09, 0A, 0B]
        mEmvKernelManager.apply {
            var capk = Hashtable<String, String>().apply {
                put(CapkTag.RID.tag, "A000000333") // UnionPay International
                put(CapkTag.INDEX.tag, "04") // UnionPay demands this CAPK to be store at the Index of 04 of the POS
                put(CapkTag.EXPONENT.tag, "03") // 0000 0011
                put(CapkTag.MODULUS.tag, "BC853E6B5365E89E7EE9317C94B02D0ABB0DBD91C05A224A2554AA29ED9FCB9D86EB9CCBB322A57811F86188AAC7351C72BD9EF196C5A01ACEF7A4EB0D2AD63D9E6AC2E7836547CB1595C68BCBAFD0F6728760F3A7CA7B97301B7E0220184EFC4F653008D93CE098C0D93B45201096D1ADFF4CF1F9FC02AF759DA27CD6DFD6D789B099F16F378B6100334E63F3D35F3251A5EC78693731F5233519CDB380F5AB8C0F02728E91D469ABD0EAE0D93B1CC66CE127B29C7D77441A49D09FCA5D6D9762FC74C31BB506C8BAE3C79AD6C2578775B95956B5370D1D0519E37906B384736233251E8F09AD79DFBE2C6ABFADAC8E4D8624318C27DAF1")
                put(CapkTag.CHECKSUM.tag, "F527081CF371DD7E1FD4FA414A665036E0F5E6E5")
            }
            updateCAPK(ContantPara.Operation.ADD, capk)

            // "9F06" - RID(Registered Application Provider Identifier): A000000333 - UnionPay International
            // "9F22" - CAPK_Index(used by the Card to specify which UPI_CAPK to use): 08
            // "DF04" - CAPK_Exponent: 03(0000 0011)
            // "DF05" - CAPK_Expiry_Date: 20401231
            // "DF02" - CAPK_MODULUS: 144 Bytes (1152 bits)
            // "DF03" - CAPK_CheckSum: 00000000000000000000000000000000000000 (19 Bytes)
            // 8D08101E7C7EF73C447615B6C59ED09C4230DF2F4B93718B99226EEF81742723E681FEFD3D11621DE5ADCB61A232F0A8C73A55D45A2720B4923F798F7A0160DD73B46C0FAA4298B13CC52E2A58C497F35904495AF73F477F353D414FECB186FF4673AD5CF8D802B232391048D59F3F6E7704145FB88005C56FDCB6A65786B35B1FDDB7051007D3E2E93A2992959F203CAE8F2C9F69909C251D5EDA12F615E16FE1FCA901D8AE9BA1EC8589CBE5D8338F4434B41AD265AB8DFD39737FFD34AB30EB36306CB0122B6A1A370D6E4342254253EAC698CD099CFB67757FDA2118F4CD144BA3DA97B1494B63D3D5FAC6BAEAE2018573C433A59BAD
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

    fun addCapkVisa(mEmvKernelManager: EmvNfcKernelApi) {
        // VISA_CAPK_INDEX: [08, 09, 53, 57, 92, 94, 96]
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

    fun addCapkMasterCard(mEmvKernelManager: EmvNfcKernelApi) {
        // MasterCard_CAPK_INDEX: [04, 05, 06, EF, F1, F3, F8, FA, FE]
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

    // <-----------------Add AID-----------------> //

    fun addAidUpiIcc(mEmvKernelManager: EmvNfcKernelApi, floorLimit: String, tacDenial: String, tacOnline: String, tacDefault: String) {
        val iccAid = Hashtable<String, String>().apply {
            put(AppTag.CARD_TYPE.tag, "IcCard")
            put(AppTag.AID.tag, "A000000333010101") // UnionPay Debit ICC
            put(AppTag.APP_VERSION.tag, "0030")
            put(AppTag.TERMINAL_FLOOR_LIMIT.tag, floorLimit)
            put(AppTag.CONTACT_TAC_DENIAL.tag, tacDenial) // Means won't trigger TAC_DENIAL at all
            put(AppTag.CONTACT_TAC_ONLINE.tag, tacOnline) // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AppTag.CONTACT_TAC_DEFAULT.tag, tacDefault)
            put(AppTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is 4 Bytes Random Number if Card doesn't specify
//            put(AppTag.DEFAULT_TDOL.tag, "9F0206") // DEFAULT TDOL(Terminal Data Object List) is 6 Bytes Amount if Card doesn't specify. The data used by Terminal to generate the HASH code
//            put(AppTag.ACQUIRER_IDENTIFIER.tag, "303030313131")
//            put(AppTag.THRESHOLD_VALUE.tag, "000000000000") // // Threshold amount of money that triggers certain logic(e.g., Need to go online); No effect in this case
//            put(AppTag.TARGET_PERCENTAGE.tag, "99") // Used by TRM to tell TAA to 99% force online (Randomly select)
//            put(AppTag.MAX_TARGET_PERCENTAGE.tag, "99") // Used by TRM to tell TAA to Up_To 99% force online (Randomly select) - Dynamically adjusting
//            put(AppTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must partially match the Application's ID
        }
        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, iccAid)
    }

    fun addAidVisaIcc(mEmvKernelManager: EmvNfcKernelApi, floorLimit: String, tacDenial: String, tacOnline: String, tacDefault: String) {
        val aid = Hashtable<String, String>().apply {
            put(AppTag.CARD_TYPE.tag, "IcCard")
            put(AppTag.AID.tag, "A0000000031010") // Visa Credit
            put(AppTag.APP_VERSION.tag, "0002")
            put(AppTag.TERMINAL_FLOOR_LIMIT.tag, floorLimit)
            put(AppTag.CONTACT_TAC_DENIAL.tag, tacDenial) // Means won't trigger TAC_DENIAL at all
            put(AppTag.CONTACT_TAC_ONLINE.tag, tacOnline) // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AppTag.CONTACT_TAC_DEFAULT.tag, tacDefault)
            put(AppTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is 4 Bytes Random Number if Card doesn't specify
//            put(AppTag.DEFAULT_TDOL.tag, "9F0206") // DEFAULT TDOL(Terminal Data Object List) is 6 Bytes Amount if Card doesn't specify. The data used by Terminal to generate the HASH code
//            put(AppTag.ACQUIRER_IDENTIFIER.tag, "303030313131")
//            put(AppTag.THRESHOLD_VALUE.tag, "000000002000") // Threshold amount of money that triggers certain logic(e.g., Need to go online). 20.00 in this case
//            put(AppTag.TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
//            put(AppTag.MAX_TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
//            put(AppTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must fully match the Application's ID
        }
        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, aid)
    }

    fun addAidMasterCardIcc(mEmvKernelManager: EmvNfcKernelApi, floorLimit: String, tacDenial: String, tacOnline: String, tacDefault: String) {
        val aid = Hashtable<String, String>().apply {
            put(AppTag.CARD_TYPE.tag, "IcCard")
            put(AppTag.AID.tag, "A0000000041010") // MasterCard Credit
            put(AppTag.APP_VERSION.tag, "0002")
            put(AppTag.TERMINAL_FLOOR_LIMIT.tag, floorLimit)
            put(AppTag.CONTACT_TAC_DENIAL.tag, tacDenial) // Means won't trigger TAC_DENIAL at all
            put(AppTag.CONTACT_TAC_ONLINE.tag, tacOnline) // 11011100 01000000 00000100 11111000 00000000; if any bit=1 hits, then will lead to Online Transaction
            put(AppTag.CONTACT_TAC_DEFAULT.tag, tacDefault)
            put(AppTag.DEFAULT_DDOL.tag, "9F3704") // DEFAULT DDOL(Dynamic Data Object List, used to format DDA/CDA Challenge) is 4 Bytes Random Number if Card doesn't specify
//            put(AppTag.DEFAULT_TDOL.tag, "9F0206") // DEFAULT TDOL is 6 Bytes Amount if Card doesn't specify
//            put(AppTag.ACQUIRER_IDENTIFIER.tag, "303030313131")
//            put(AppTag.THRESHOLD_VALUE.tag, "000000002000") // Threshold amount of money that triggers certain logic(e.g., Need to go online). 20.00 in this case
//            put(AppTag.TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
//            put(AppTag.MAX_TARGET_PERCENTAGE.tag, "00") // No transaction needs to be forced to go online randomly
//            put(AppTag.APP_SELECT_INDICATOR.tag, "00") // SELECT AID must fully match the Application's ID
        }
        mEmvKernelManager.updateAID(ContantPara.Operation.ADD, aid)
    }




}