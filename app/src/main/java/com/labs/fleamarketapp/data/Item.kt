package com.labs.fleamarketapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val category: String,
    val sellerId: String,
    val sellerName: String,
    val condition: String = "GOOD",
    val pickupLocation: String = "STC",
    val createdAt: Long,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val isAuction: Boolean = false,
    val auctionEndTime: Long? = null,
    val currentBid: Double? = null
) : Parcelable

enum class ItemStatus {
    AVAILABLE,
    SOLD,
    RESERVED
}

