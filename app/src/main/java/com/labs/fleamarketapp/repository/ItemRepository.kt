package com.labs.fleamarketapp.repository

import com.labs.fleamarketapp.api.ApiClient
import com.labs.fleamarketapp.api.models.CreateItemRequest
import com.labs.fleamarketapp.api.models.ServerItem
import com.labs.fleamarketapp.local.dao.CategoryDao
import com.labs.fleamarketapp.local.dao.ItemDao
import com.labs.fleamarketapp.local.entities.CategoryEntity
import com.labs.fleamarketapp.local.entities.ItemEntity
import com.labs.fleamarketapp.local.entities.ItemCondition
import com.labs.fleamarketapp.local.entities.ItemType
import com.labs.fleamarketapp.local.entities.Status
import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao
) {
    
    private val api = ApiClient.api
    suspend fun upsertItem(item: ItemEntity) = itemDao.upsert(item)
    fun getItem(id: String): Flow<ItemEntity?> = itemDao.getById(id)
    fun getActiveItems(): Flow<List<ItemEntity>> = itemDao.getByStatus(Status.ACTIVE)
    fun getFeaturedItems(): Flow<List<ItemEntity>> = itemDao.getByStatus(Status.ACTIVE)
    fun getItemsByCategory(categoryId: Long): Flow<List<ItemEntity>> = itemDao.getByCategoryAndStatus(categoryId, Status.ACTIVE)
    fun searchItems(query: String): Flow<List<ItemEntity>> = itemDao.search(query)
    fun search(query: String): Flow<List<ItemEntity>> = itemDao.search(query)
    fun getByType(type: ItemType): Flow<List<ItemEntity>> = itemDao.getByType(type)

    suspend fun upsertCategory(category: CategoryEntity) = categoryDao.upsert(category)
    fun getCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()
    
    suspend fun createItem(
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
    ): Result<ItemEntity> {
        return try {
            val request = CreateItemRequest(
                title = title,
                description = description,
                price = price,
                startingBid = startingBid,
                condition = condition,
                itemType = itemType,
                images = images,
                categoryId = categoryId?.toString(),
                auctionEndTime = auctionEndTime,
                pickupLocation = pickupLocation
            )
            
            val response = api.createItem(sellerId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val serverItem = response.body()?.data
                if (serverItem != null) {
                    val entity = ItemEntity(
                        id = serverItem.id,
                        sellerId = serverItem.sellerId,
                        title = serverItem.title,
                        description = serverItem.description,
                        price = serverItem.price,
                        startingBid = serverItem.startingBid,
                        currentBid = serverItem.currentBid,
                        condition = ItemCondition.valueOf(serverItem.condition),
                        itemType = ItemType.valueOf(serverItem.itemType),
                        status = Status.valueOf(serverItem.status),
                        images = serverItem.images,
                        categoryId = serverItem.categoryId?.toLongOrNull(),
                        auctionEndTime = serverItem.auctionEndTime,
                        pickupLocation = serverItem.pickupLocation,
                        createdAt = serverItem.createdAt
                    )
                    itemDao.upsert(entity)
                    Result.success(entity)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to create item"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to create item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshItems(
        categoryId: Long? = null,
        searchQuery: String? = null,
        sellerId: String? = null
    ): Result<List<ItemEntity>> {
        return try {
            val response = api.getItems(
                category = categoryId?.toString(),
                search = searchQuery,
                sellerId = sellerId
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val serverItems = response.body()?.data ?: emptyList()
                val entities = serverItems.map { it.toEntity() }
                
                if (categoryId == null && searchQuery == null && sellerId == null) {
                    itemDao.clearAll()
                    itemDao.upsertAll(entities)
                } else {
                    itemDao.upsertAll(entities)
                }
                Result.success(entities)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to refresh items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchRemote(query: String): Result<List<ItemEntity>> =
        refreshItems(searchQuery = query)
    
    suspend fun getItemsForSeller(sellerId: String): Result<List<ItemEntity>> =
        refreshItems(sellerId = sellerId)
    
    private fun ServerItem.toEntity(): ItemEntity {
        return ItemEntity(
            id = id,
            sellerId = sellerId,
            title = title,
            description = description,
            price = price,
            startingBid = startingBid,
            currentBid = currentBid,
            condition = ItemCondition.valueOf(condition),
            itemType = ItemType.valueOf(itemType),
            status = Status.valueOf(status),
            images = images,
            categoryId = categoryId?.toLongOrNull(),
            auctionEndTime = auctionEndTime,
            createdAt = createdAt
        )
    }
}
