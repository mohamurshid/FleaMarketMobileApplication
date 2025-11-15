package com.labs.fleamarketapp.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enums used across entities
enum class UserRole { BUYER, SELLER, ADMIN }
enum class ItemType { FIXED_PRICE, AUCTION }
enum class ItemCondition { NEW, LIKE_NEW, GOOD, FAIR }
enum class Status { PENDING, APPROVED, ACTIVE, SOLD, COMPLETED }
enum class NotificationType { SYSTEM, BID, ORDER, INFO }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val status: Status
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val title: String,
    val description: String,
    val price: Double?, // nullable for auction-only items
    val startingBid: Double? = null,
    val currentBid: Double? = null,
    val condition: ItemCondition,
    val itemType: ItemType,
    val status: Status,
    val images: List<String>, // type converter required
    val categoryId: Long? = null,
    val auctionEndTime: Long? = null,
    val pickupLocation: String = "STC",
    val createdAt: Long // added to support ORDER BY createdAt queries
)

@Entity(tableName = "bids")
data class BidEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val bidderId: String,
    val amount: Double,
    val timestamp: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val buyerId: String,
    val sellerId: String,
    val totalAmount: Double,
    val status: Status,
    val createdAt: Long,
    val selectedPickupLocation: String = "STC"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean,
    val timestamp: Long
)
