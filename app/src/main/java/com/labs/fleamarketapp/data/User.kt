package com.labs.fleamarketapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String,
    val phone: String? = null,
    val profileImageUrl: String? = null,
    val rating: Float = 0f,
    val userType: UserType = UserType.BUYER,
    val status: String = "PENDING",
    val authToken: String? = null
) : Parcelable

enum class UserType {
    BUYER,
    SELLER,
    ADMIN
}

