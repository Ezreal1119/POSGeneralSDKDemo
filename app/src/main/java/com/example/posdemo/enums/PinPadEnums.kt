package com.example.posdemo.enums

enum class Dukpt(val index: Int) {
    MSR(1),
    EMV(2),
    PIN(3),
    MAC(4)
}

enum class PinParams(val tag: String) {
    CARD_NO("cardNo"),
    TITLE("title"),
    MESSAGE("message"),
    INFO_LOCATION("infoLocation"),
    SOUND("sound"),
    BYPASS("bypass"),
    SUPPORT_PIN_LEN("supportPinLen"),
    FULL_SCREEN("FullScreen"),
    ONLINE_PIN("onlinePin"), // If online, then set to true. If offline, then set to false.
    TIMEOUT_MS("timeOutMS"),
    RANDOM_KEYBOARD("randomKeyboard"),
    RANDOM_KEYBOARD_LOCATION("randomKeyboardLocation"),
    PIN_KEY_NO("PINKeyNo"),
    INPUT_BY_SECURITY_PIN_PAD("inputBySP"), // IF true, means APP won't receive any KeyEvent at all, and Custom UI won't take effect.

    // Offline PIN
    INPUT_TYPE("inputType"), // 3: Plain; 4: Enciphered
    CARD_SLOT("CardSlot"), // fixed to 0
    MODULUS("Module"),
    MODULUS_LEN("ModuleLen"),
    EXPONENT("Exponent"),
    EXPONENT_LEN("ExponentLen"),


    // Custom Keypad
    CUSTOMIZATION("customization"),
    STR_JSON("strJson"),
    BACKGROUND_COLOR("backgroundColor"),
    TEXT_COLOR("textColor"),
    CANCEL_BITMAP("cancelBitmap"),
    DELETE_BITMAP("delBitmap"),
    OK_BITMAP("okBitmap"),
    BACKSPACE_BITMAP("backspaceBitmap"),
    VIEW_BITMAP("viewBitmap"),
    BODY_BITMAP("bodyBitmap")
}

enum class Tr31Params(val tag: String) {
    KBPK_MK_INDEX("keyNo"),
    KEY_WK_LOAD_INDEX("sKeyNo"),
    TR31_KEY_BLOCK_IN_BYTES("content"),
    TR31_KEY_BLOCK_LEN("content_size")
}