package com.labs.fleamarketapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.labs.fleamarketapp.data.Item
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.local.db.MarketplaceDatabase
import com.labs.fleamarketapp.local.entities.ItemEntity
import com.labs.fleamarketapp.repository.ItemRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ItemViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = MarketplaceDatabase.getInstance(application)
    private val itemRepository = ItemRepository(
        itemDao = database.itemDao(),
        categoryDao = database.categoryDao()
    )
    
    private val _itemsState = MutableLiveData<UiState<List<Item>>>()
    val itemsState: LiveData<UiState<List<Item>>> = _itemsState
    
    private val _featuredItemsState = MutableLiveData<UiState<List<Item>>>()
    val featuredItemsState: LiveData<UiState<List<Item>>> = _featuredItemsState
    
    private val _userListingsState = MutableLiveData<UiState<List<Item>>>()
    val userListingsState: LiveData<UiState<List<Item>>> = _userListingsState
    
    private val _createItemState = MutableLiveData<UiState<Item>>()
    val createItemState: LiveData<UiState<Item>> = _createItemState
    
    private val _refreshState = MutableLiveData<UiState<Unit>>()
    val refreshState: LiveData<UiState<Unit>> = _refreshState
    
    /**
     * Load featured items - refresh from API first, fallback to local cache if offline
     */
    fun loadFeaturedItems() {
        viewModelScope.launch {
            _featuredItemsState.value = UiState.Loading
            val result = itemRepository.refreshItems()
            result.onSuccess { entities ->
                _featuredItemsState.value = UiState.Success(entities.map { it.toItem() })
            }.onFailure { error ->
                val fallback = itemRepository.getFeaturedItems().firstOrNull().orEmpty()
                if (fallback.isNotEmpty()) {
                    _featuredItemsState.value = UiState.Success(fallback.map { it.toItem() })
                } else {
                    _featuredItemsState.value = UiState.Error(error.message ?: "Failed to load items")
                }
            }
        }
    }
    
    /**
     * Search items across the shared backend
     */
    fun searchItems(query: String) {
        if (query.isBlank()) {
            loadFeaturedItems()
            return
        }
        
        viewModelScope.launch {
            _itemsState.value = UiState.Loading
            val result = itemRepository.searchRemote(query)
            result.onSuccess { entities ->
                _itemsState.value = UiState.Success(entities.map { it.toItem() })
            }.onFailure { error ->
                val fallback = itemRepository.searchItems(query).firstOrNull().orEmpty()
                if (fallback.isNotEmpty()) {
                    _itemsState.value = UiState.Success(fallback.map { it.toItem() })
                } else {
                    _itemsState.value = UiState.Error(error.message ?: "Search failed")
                }
            }
        }
    }
    
    /**
     * Get items by category from shared backend
     */
    fun getItemsByCategory(categoryId: Long) {
        viewModelScope.launch {
            _itemsState.value = UiState.Loading
            val result = itemRepository.refreshItems(categoryId = categoryId)
            result.onSuccess { entities ->
                _itemsState.value = UiState.Success(entities.map { it.toItem() })
            }.onFailure { error ->
                val fallback = itemRepository.getItemsByCategory(categoryId).firstOrNull().orEmpty()
                if (fallback.isNotEmpty()) {
                    _itemsState.value = UiState.Success(fallback.map { it.toItem() })
                } else {
                    _itemsState.value = UiState.Error(error.message ?: "Failed to load items")
                }
            }
        }
    }
    
    /**
     * Create item listing (local only)
     */
    fun createItem(
        sellerId: String,
        title: String,
        description: String,
        price: Double?,
        startingBid: Double?,
        condition: String,
        itemType: String,
        images: List<String>,
        categoryId: Long?,
        auctionEndTime: Long?,
        pickupLocation: String
    ) {
        viewModelScope.launch {
            _createItemState.value = UiState.Loading
            val result = itemRepository.createItem(
                sellerId = sellerId,
                title = title,
                description = description,
                price = price,
                startingBid = startingBid,
                condition = condition,
                itemType = itemType,
                images = images,
                categoryId = categoryId,
                auctionEndTime = auctionEndTime,
                pickupLocation = pickupLocation
            )
            
            result.onSuccess { entity ->
                _createItemState.value = UiState.Success(entity.toItem())
            }.onFailure { e ->
                _createItemState.value = UiState.Error(e.message ?: "Failed to create item")
            }
        }
    }
    
    /**
     * Pull-to-refresh: Reload items from local database
     */
    fun refreshItems() {
        viewModelScope.launch {
            _refreshState.value = UiState.Loading
            val result = itemRepository.refreshItems()
            
            result.onSuccess {
                _refreshState.value = UiState.Success(Unit)
                loadFeaturedItems() // Reload after refresh
            }.onFailure { e ->
                _refreshState.value = UiState.Error(e.message ?: "Refresh failed")
            }
        }
    }
    
    /**
     * Load listings created by the current user from the shared backend
     */
    fun loadUserListings(userId: String) {
        viewModelScope.launch {
            _userListingsState.value = UiState.Loading
            val result = itemRepository.getItemsForSeller(userId)
            result.onSuccess { entities ->
                _userListingsState.value = UiState.Success(entities.map { it.toItem() })
            }.onFailure { error ->
                val fallback = itemRepository.getActiveItems().firstOrNull().orEmpty()
                    .filter { it.sellerId == userId }
                if (fallback.isNotEmpty()) {
                    _userListingsState.value = UiState.Success(fallback.map { it.toItem() })
                } else {
                    _userListingsState.value = UiState.Error(error.message ?: "Failed to load listings")
                }
            }
        }
    }
    
    // Helper to convert ItemEntity to Item (UI model)
    private fun ItemEntity.toItem(): Item = Item(
        id = id,
        title = title,
        description = description,
        price = (price ?: startingBid ?: 0.0),
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

