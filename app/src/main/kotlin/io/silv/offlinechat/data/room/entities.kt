package io.silv.offlinechat.data.room

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import io.silv.offlinechat.Chat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.UUID

@Entity
data class User(
    @PrimaryKey(autoGenerate = false) val mac: String,
    @ColumnInfo(name = "user_name") val name: String,
)

@Entity
data class Conversation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "participants") val participantMacList: List<String> = emptyList(),
    @ColumnInfo(name ="chats") val chats: List<Chat> = emptyList()
)

class ConversationTypeConverters {

    private val serializers = listOf(
        Chat.SentMessage.serializer(),
        Chat.SentImage.serializer(),
        Chat.ReceivedImage.serializer(),
        Chat.ReceivedMessage.serializer()
    )

    @TypeConverter
    fun stringToChat(s: String): Chat {
        for (serializer in serializers) {
             runCatching { Json.decodeFromString(serializer, s) }
                 .onSuccess { return it }
        }
        return Chat.SentMessage("", 0)
    }

    @TypeConverter
    fun chatToString(chat: Chat): String {
        runCatching { Json.encodeToString(chat) }
            .onSuccess { return it }
        return ""
    }
}

object UriAsStringSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Uri {
        val string = decoder.decodeString()
        return string.toUri()
    }
}