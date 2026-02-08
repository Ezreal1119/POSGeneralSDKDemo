package com.example.posdemo.enums


enum class LedColor(val id: Int, val color: String) {
    BLUE(1, "Blue"),
    YELLOW(2, "Yellow"),
    GREEN(3, "Green"),
    RED(4, "Red");

    override fun toString(): String {
        return color
    }
}