package io.silv.offlinechat.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(vararg user: User)

    @Query("""
       SELECT * FROM user
       WHERE :mac LIKE mac
       LIMIT 1
    """)
    suspend fun getUserByMac(mac: String): User

    @Query("""
        SELECT * FROM USER
    """)
    fun getAllSeenUsers(): Flow<List<User>>
}