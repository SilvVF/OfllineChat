package io.silv.offlinechat.data.room

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseRepo(
   private val db: OfflineChatDatabase
) {

    private val users = db.getUserDao()
    private val conversations = db.getConversationDao()

    suspend fun insertUserIntoDb(vararg user: User) = withContext(Dispatchers.IO) {
        user.forEach { users.upsertUser(it) }
    }
}