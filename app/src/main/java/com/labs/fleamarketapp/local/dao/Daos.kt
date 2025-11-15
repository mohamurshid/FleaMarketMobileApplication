package com.labs.fleamarketapp.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.labs.fleamarketapp.local.entities.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<UserEntity>>

    @Delete
    suspend fun delete(user: UserEntity)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name")
    fun getAll(): Flow<List<CategoryEntity>>
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)
    
    @Query("DELETE FROM items")
    suspend fun clearAll()

    @Query("SELECT * FROM items WHERE id = :id")
    fun getById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE status = :status ORDER BY title")
    fun getByStatus(status: Status): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE categoryId = :categoryId AND status = :status ORDER BY title")
    fun getByCategoryAndStatus(categoryId: Long, status: Status): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY title")
    fun search(query: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE itemType = :type ORDER BY createdAt DESC")
    fun getByType(type: ItemType): Flow<List<ItemEntity>>

    @Delete
    suspend fun delete(item: ItemEntity)
}

@Dao
interface BidDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun placeBid(bid: BidEntity)

    @Query("SELECT * FROM bids WHERE itemId = :itemId ORDER BY amount DESC, timestamp ASC")
    fun getBidsForItem(itemId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE itemId = :itemId ORDER BY amount DESC LIMIT 1")
    suspend fun getHighestBid(itemId: String): BidEntity?
}

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getById(id: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE buyerId = :userId OR sellerId = :userId ORDER BY createdAt DESC")
    fun getOrdersForUser(userId: String): Flow<List<OrderEntity>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity)
    
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getForUser(userId: String): Flow<List<NotificationEntity>>
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}

// Local entities DAOs for user-specific data
@Dao
interface LocalItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: com.labs.fleamarketapp.local.entities.LocalItem)
    
    @Query("SELECT * FROM local_items WHERE userId = :userId ORDER BY lastModified DESC")
    fun getDraftsForUser(userId: String): Flow<List<com.labs.fleamarketapp.local.entities.LocalItem>>
    
    @Query("SELECT * FROM local_items WHERE id = :id")
    suspend fun getById(id: String): com.labs.fleamarketapp.local.entities.LocalItem?
    
    @Delete
    suspend fun delete(item: com.labs.fleamarketapp.local.entities.LocalItem)
    
    @Query("DELETE FROM local_items WHERE userId = :userId AND isDraft = 0")
    suspend fun clearSyncedItems(userId: String)
}

@Dao
interface DraftBidDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bid: com.labs.fleamarketapp.local.entities.DraftBid)
    
    @Query("SELECT * FROM draft_bids WHERE userId = :userId AND synced = 0")
    fun getUnsyncedBids(userId: String): Flow<List<com.labs.fleamarketapp.local.entities.DraftBid>>
    
    @Query("UPDATE draft_bids SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Delete
    suspend fun delete(bid: com.labs.fleamarketapp.local.entities.DraftBid)
}

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prefs: com.labs.fleamarketapp.local.entities.UserPreferences)
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getForUser(userId: String): com.labs.fleamarketapp.local.entities.UserPreferences?
}

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: com.labs.fleamarketapp.local.entities.CartItem)
    
    @Query("SELECT * FROM shopping_cart WHERE userId = :userId")
    fun getCartForUser(userId: String): Flow<List<com.labs.fleamarketapp.local.entities.CartItem>>
    
    @Delete
    suspend fun delete(item: com.labs.fleamarketapp.local.entities.CartItem)
    
    @Query("DELETE FROM shopping_cart WHERE userId = :userId")
    suspend fun clearCart(userId: String)
}
