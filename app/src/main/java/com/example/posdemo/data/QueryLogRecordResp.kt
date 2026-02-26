package com.example.posdemo.data

data class QueryLogRecordResp(
    val code: Int,
    val data: List<LogItem>?
)

data class LogItem(
    val logType: Int,
    val logTaskContent: String?,
    val createTime: String
)