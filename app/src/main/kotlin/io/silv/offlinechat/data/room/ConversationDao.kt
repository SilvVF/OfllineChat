package io.silv.offlinechat.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("""
        SELECT * FROM conversation where :id LIKE id LIMIT 1
    """)
    suspend fun getConversationById(id: String): Conversation

    @Query("""
        SELECT * FROM conversation where :id LIKE id
    """)
    fun getConversationsById(vararg id: String): Flow<List<Conversation>>


    @Query("""
            SELECT * from conversation
            WHERE :mac IN (participants)
    """)
    fun findConversationsByMacAddress(mac: String): Flow<List<Conversation>>


    @Query("""
        DELETE FROM conversation WHERE :id LIKE id
    """)
    suspend fun deleteConversationById(id: String)

    @Delete
    suspend fun deleteConversation(vararg conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConversation(vararg conversation: Conversation)

}