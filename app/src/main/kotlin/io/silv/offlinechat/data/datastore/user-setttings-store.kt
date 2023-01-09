package io.silv.offlinechat.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import io.silv.offlinechat.datastore.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

interface UserSettingsDataStoreRepo {
    val nameFlow: Flow<String>
    suspend fun writeName(name: String): UserSettings
    val ageFlow: Flow<Int>
    suspend fun writeAge(age: Int): UserSettings
}

class UserSettingsDataStoreRepoImpl(private val store: DataStore<UserSettings>): UserSettingsDataStoreRepo {

    override val nameFlow: Flow<String> = store.data.map { userSettings ->  userSettings.name }

    override suspend fun writeName(name: String) = store.updateData { userSettings ->
        userSettings.toBuilder()
            .setName(name)
            .build()
    }

    override val ageFlow: Flow<Int> = store.data.map { userSettings -> userSettings.age }

    override suspend fun writeAge(age: Int) = store.updateData { userSettings ->
        userSettings.toBuilder()
            .setAge(age)
            .build()
    }
}

object UserSettingsSerializer: Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserSettings {
        try {
            return UserSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) = t.writeTo(output)
}

val Context.userSettingsDataStore: DataStore<UserSettings> by dataStore(
    fileName = "UserSettings.pb",
    serializer = UserSettingsSerializer
)