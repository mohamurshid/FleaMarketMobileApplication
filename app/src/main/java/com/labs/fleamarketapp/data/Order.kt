package com.labs.fleamarketapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val buyerId: String,
    val sellerId: String,
    val amount: Double,
    val status: OrderStatus,
    val createdAt: Long,
    val completedAt: Long? = null,
    val selectedPickupLocation: String = "STC"
) : Parcelable

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

