package io.silv.offlinechat.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(
    entities = [ConversationDao::class, UserDao::class],
    version = 1,
)
@TypeConverters(ConversationTypeConverters::class)
abstract class OfflineChatDatabase: RoomDatabase() {

    abstract fun getConversationDao(): ConversationDao

    abstract fun getUserDao(): UserDao

}