package com.example.posdemo.enums

import com.urovo.i9000s.api.emv.ContantPara

enum class TerminalTag(val tag: String, val len: String) {
    COUNTRY_CODE("9F1A", "02"), // 2 bytes HEX, e.g. "0724"(Spain)
    CURRENCY_CODE("5F2A", "02"), // 2 bytes HEX, e.g. "0156"(CNY); "0840"(USD); "0978"(EUR)
    TRANSACTION_CURRENCY_EXPONENT("5F36", "01"), // 1 byte HEX, e.g. "02"
    TERMINAL_TYPE("9F35", "01"), // 1 byte HEX, e.g. "0x22"(OnlinePOS); "0x14"(OfflinePOS)
    TERMINAL_CAPABILITIES("9F33", "03"), // 3 bytes HEX, e.g. "E0F0C8"(3 * 8bits) -> CVM / Online-Offline / PaymentMethod
    ADDITIONAL_TERMINAL_CAPABILITIES("9F40", "05"), // 5 bytes HEX, e.g. "6000F0A001"
    RANDOM_TRANSACTION_SWITCH("DF02", "01"), // 1 Byte, e.g. 01
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
enum class AppTag(val tag: String) {
    // In common
    CARD_TYPE("CardType"), // e.g. "IcCard"(ICC); "UpiCard"(PICC)

    // ICC
    AID("aid"), // ICC: e.g. "A0000000031010"(Visa Credit); "A0000000041010"(MasterCard Credit)
    APP_VERSION("appVersion"), // 9F09 e.g. "0002"
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

    // PICC
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

enum class IssuerResp(val display: String) {
    APPROVAL("APPROVAL(3030)"), // ARC(8A): 3030
    DECLINE("DECLINE(3035)")   // ARC(8A): 3035 or other codes not 3030;
    ;

    override fun toString(): String {
        return display
    }}

enum class EmvBundle(val tag: String) {
    PUBLIC_KEY_MODULUS("pub"),
    PUBLIC_KEY_MODULUS_LEN("publen"),
    PUBLIC_KEY_EXPONENT("exp"),
    PUBLIC_KEY_EXPONENT_LEN("explen")
}

enum class Amount(val amount: String, val display: String) {
    UNDER_THRESHOLD("50.00", "50.00(50%)"),
    MIDDLE_BETWEEN_THRESHOLD_AND_FLOOR("180.00", "180.00(90%)"),
    ABOVE_FLOOR("500.00", "500.00(100%)"),
    ENTER_AFTER_READ_RECORD("ENTER_AFTER_READ_RECORD", "ENTER AFTER RR");

    override fun toString(): String {
        return display
    }
}

enum class EmvOptions(val value: ContantPara.EmvOption, val display: String) {
    START(ContantPara.EmvOption.START, "Normal Start"),
    START_WITH_FORCE_ONLINE(ContantPara.EmvOption.START_WITH_FORCE_ONLINE, "Force Online");

    override fun toString(): String {
        return display
    }}

enum class FallbackSwitch(val value: String, val display: String) {
    DISABLED("0", "Fallback Off"),
    ENABLED("1", "Fallback On"), ;

    override fun toString(): String {
        return display
    }
}