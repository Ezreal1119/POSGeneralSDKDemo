package com.example.posdemo.data

data class ConfigResp(
    val code: Int,
    val data: List<ConfigItem>?
)

data class ConfigItem(
    val configType: String,
    val configCode: String,
    val pushTime: String?
)