package com.labs.fleamarketapp.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.labs.fleamarketapp.local.dao.*
import com.labs.fleamarketapp.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        BidEntity::class,
        OrderEntity::class,
        CategoryEntity::class,
        NotificationEntity::class,
        com.labs.fleamarketapp.local.entities.LocalItem::class,
        com.labs.fleamarketapp.local.entities.DraftBid::class,
        com.labs.fleamarketapp.local.entities.UserPreferences::class,
        com.labs.fleamarketapp.local.entities.CartItem::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MarketplaceDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun bidDao(): BidDao
    abstract fun orderDao(): OrderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun localItemDao(): com.labs.fleamarketapp.local.dao.LocalItemDao
    abstract fun draftBidDao(): com.labs.fleamarketapp.local.dao.DraftBidDao
    abstract fun userPreferencesDao(): com.labs.fleamarketapp.local.dao.UserPreferencesDao
    abstract fun cartDao(): com.labs.fleamarketapp.local.dao.CartDao

    companion object {
        @Volatile private var INSTANCE: MarketplaceDatabase? = null

        fun getInstance(context: Context): MarketplaceDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                MarketplaceDatabase::class.java,
                "marketplace.db"
            )
            .addCallback(PrepopulateCallback())
            .fallbackToDestructiveMigration()
            .build()
                .also { INSTANCE = it }
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.execSQL("INSERT INTO users(id,email,firstName,lastName,role,status) VALUES('admin','admin@strathmore.edu','Admin','User','ADMIN','APPROVED')")
                    db.execSQL("INSERT INTO categories(id,name,description) VALUES(1,'Electronics','Electronics and gadgets')")
                    db.execSQL("INSERT INTO categories(id,name,description) VALUES(2,'Books','Books and stationery')")
                } catch (_: Exception) { }
            }
        }
    }
}
