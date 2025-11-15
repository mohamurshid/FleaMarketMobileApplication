package com.labs.fleamarketapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.labs.fleamarketapp.data.Bid
import com.labs.fleamarketapp.data.Item
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.local.db.MarketplaceDatabase
import com.labs.fleamarketapp.local.entities.BidEntity
import com.labs.fleamarketapp.repository.AuctionRepository
import com.labs.fleamarketapp.repository.ItemRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuctionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = MarketplaceDatabase.getInstance(application)
    private val itemRepository = ItemRepository(
        itemDao = database.itemDao(),
        categoryDao = database.categoryDao()
    )
    private val bidRepository = AuctionRepository(
        bidDao = database.bidDao()
    )
    
    private val _itemState = MutableLiveData<UiState<Item>>()
    val itemState: LiveData<UiState<Item>> = _itemState
    
    private val _bidsState = MutableLiveData<UiState<List<Bid>>>()
    val bidsState: LiveData<UiState<List<Bid>>> = _bidsState
    
    private val _placeBidState = MutableLiveData<UiState<Bid>>()
    val placeBidState: LiveData<UiState<Bid>> = _placeBidState
    
    /**
     * Load item details from local database
     */
    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _itemState.value = UiState.Loading
            itemRepository.getItem(itemId)
                .catch { e ->
                    _itemState.value = UiState.Error(e.message ?: "Failed to load item")
                }
                .collect { entity ->
                    if (entity != null) {
                        _itemState.value = UiState.Success(entity.toItem())
                    } else {
                        _itemState.value = UiState.Error("Item not found")
                    }
                }
        }
    }
    
    /**
     * Load bids for an item (from local database)
     */
    fun loadBids(itemId: String) {
        viewModelScope.launch {
            _bidsState.value = UiState.Loading
            bidRepository.getBidsForItem(itemId)
                .catch { e ->
                    _bidsState.value = UiState.Error(e.message ?: "Failed to load bids")
                }
                .collect { entities ->
                    val bids = entities.map { it.toBid() }
                    _bidsState.value = UiState.Success(bids)
                }
        }
    }
    
    /**
     * Place a bid (local only)
     */
    fun placeBid(itemId: String, bidderId: String, amount: Double) {
        viewModelScope.launch {
            _placeBidState.value = UiState.Loading
            val result = bidRepository.placeBid(
                itemId = itemId,
                bidderId = bidderId,
                amount = amount
            )
            
            result.onSuccess { entity ->
                _placeBidState.value = UiState.Success(entity.toBid())
                loadBids(itemId) // Refresh bids list
            }.onFailure { e ->
                _placeBidState.value = UiState.Error(e.message ?: "Failed to place bid")
            }
        }
    }
    
    // Helper to convert BidEntity to Bid
    private fun BidEntity.toBid(): Bid {
        return Bid(
            id = id,
            itemId = itemId,
            bidderId = bidderId,
            bidderName = "", // Will be populated from server
            amount = amount,
            timestamp = timestamp
        )
    }
    
    // Helper to convert ItemEntity to Item (duplicate from ItemViewModel - consider moving to a mapper)
    private fun com.labs.fleamarketapp.local.entities.ItemEntity.toItem(): Item = Item(
        id = id,
        title = title,
        description = description,
        price = price ?: startingBid ?: 0.0,
        imageUrl = images.firstOrNull(),
        images = images,
        category = "",
        sellerId = sellerId,
        sellerName = "",
        pickupLocation = pickupLocation,
        createdAt = createdAt,
        status = when (status) {
            com.labs.fleamarketapp.local.entities.Status.ACTIVE -> com.labs.fleamarketapp.data.ItemStatus.AVAILABLE
            com.labs.fleamarketapp.local.entities.Status.SOLD -> com.labs.fleamarketapp.data.ItemStatus.SOLD
            else -> com.labs.fleamarketapp.data.ItemStatus.RESERVED
        },
        isAuction = itemType == com.labs.fleamarketapp.local.entities.ItemType.AUCTION,
        auctionEndTime = auctionEndTime,
        currentBid = currentBid ?: price ?: startingBid,
        condition = condition.name
    )
}

