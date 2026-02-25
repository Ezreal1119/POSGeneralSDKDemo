package com.example.posdemo.data

data class AppListResp(
    val code: Int,
    val data: List<AppItem>?
)

data class AppItem(
    val appName: String?,
    val appPackage: String?,
    val appUrl: String?,
    val appVersionName: String?,
    val appVersionCode: String?
)