package com.example.posdemo.data

data class Pinpad(
    val key_0: Key? = null,
    val key_1: Key? = null,
    val key_2: Key? = null,
    val key_3: Key? = null,
    val key_4: Key? = null,
    val key_5: Key? = null,
    val key_6: Key? = null,
    val key_7: Key? = null,
    val key_8: Key? = null,
    val key_9: Key? = null,

    var key_cancel: Key? = null,
    var key_del: Key? = null,
    var key_ok: Key? = null,
    var key_continue: Key? = null,
    var key_blank1: Key? = null,
    var key_blank2: Key? = null,

    val title: Message? = null,
    val head: Message? = null,
    val money: Message? = null,
    val info: Message? = null,

    val echo: Echo? = null,
    val view: Echo? = null,
    val imageview: Echo? = null,

    val body_imageview: PinpadArea? = null,
    val key_imageview: PinpadArea? = null,

    val body_area: PinpadArea? = null,
    val key_area: PinpadArea? = null,
    val backspace: PinpadArea? = null,

    val echo_imageviews: EchoImageviews? = null,

    var translations: Translations? = null
)

data class Key(
    val left: Int = 0,
    val top: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null,
    var text: String? = null,
    val color: String? = null,
    val fontSize: Int = 0,
    val display: String? = null,
    val borderStyle: String? = null,
    val borderWidth: Int = 0,
    val borderRadius: Int = 0,
    val borderColor: String? = null
)

data class Message(
    val left: Int = 0,
    val top: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null,
    val text: String? = null,
    val color: String? = null,
    val fontSize: Int = 0,
    val display: String? = null,
    val borderStyle: String? = null,
    val borderWidth: Int = 0,
    val borderRadius: Int = 0,
    val borderColor: String? = null
)

data class Echo(
    val left: Int = 0,
    val top: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null,
    val display: String? = null,
    val borderStyle: String? = null,
    val borderWidth: Int = 0,
    val borderRadius: Int = 0,
    val borderColor: String? = null
)

data class PinpadArea(
    val left: Int = 0,
    val top: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null
)

data class EchoImageviews(
    val left: Int = 0,
    val marginLeft: Int = 0,
    val top: Int = 0,
    val height: Int = 0,
    val width: Int = 0,
    val backgroundImage: String? = null,
    val backgroundColor: String? = null
)


data class Translations(
    val pinpad_below: String? = null,
    val pinpad_blank_click_tip: String? = null,
    val pinpad_input_less: String? = null,
    val pinpad_input_more: String? = null,
    val password_confirm: String? = null,
    val password_cancel: String? = null,

    val has_selected_one: String? = null,
    val has_selected_two: String? = null,
    val has_selected_three: String? = null,
    val has_selected_four: String? = null,
    val has_selected_five: String? = null,
    val has_selected_six: String? = null,
    val has_selected_seven: String? = null,
    val has_selected_eight: String? = null,
    val has_selected_nine: String? = null,
    val has_selected_ten: String? = null,
    val has_selected_eleven: String? = null,
    val has_selected_twelve: String? = null
)