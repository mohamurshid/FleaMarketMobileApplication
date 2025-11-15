package com.labs.fleamarketapp.repository

import com.labs.fleamarketapp.api.ApiClient
import com.labs.fleamarketapp.api.models.PlaceBidRequest
import com.labs.fleamarketapp.local.dao.BidDao
import com.labs.fleamarketapp.local.entities.BidEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AuctionRepository(private val bidDao: BidDao) {
    
    private val api = ApiClient.api
    
    suspend fun placeBid(bid: BidEntity) = bidDao.placeBid(bid)
    fun getBidsForItem(itemId: String): Flow<List<BidEntity>> = bidDao.getBidsForItem(itemId)
    suspend fun getHighestBid(itemId: String): BidEntity? = bidDao.getHighestBid(itemId)
    
    suspend fun placeBid(
        itemId: String,
        bidderId: String,
        amount: Double
    ): Result<BidEntity> {
        return try {
            val request = PlaceBidRequest(amount = amount)
            val response = api.placeBid(bidderId, itemId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val serverBid = response.body()?.data
                if (serverBid != null) {
                    val bid = BidEntity(
                        id = serverBid.id,
                        itemId = serverBid.itemId,
                        bidderId = serverBid.bidderId,
                        amount = serverBid.amount,
                        timestamp = serverBid.timestamp
                    )
                    bidDao.placeBid(bid)
                    Result.success(bid)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to place bid"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to place bid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
