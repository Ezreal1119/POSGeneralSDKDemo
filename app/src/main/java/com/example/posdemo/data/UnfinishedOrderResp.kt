package com.example.posdemo.data

data class UnfinishedOrderResp(
    val code: Int,
    val data: List<OrderItem>?
)

data class OrderItem(
    val id: String,
    val orderId: String,
    val orderType: String,
    val orderContent: String?,
    val startTime: String?
)