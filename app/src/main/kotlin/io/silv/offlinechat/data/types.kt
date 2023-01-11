package io.silv.offlinechat.data

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
abstract class SocketData(
    val type: String,
)

interface LocalData

@Serializable
data class Ack(
    val mac: String ="",
    val name: String=""
): SocketData("ack")

data class ChatRequest(
    val mac: String,
    val name: String
): LocalData

@Serializable
data class Message(
    val content: String,
    val sender: String,
    val time: Long = System.currentTimeMillis()
) : SocketData("message"), LocalData

@Serializable
data class Image(
    val bytes: ByteArray,
    val sender: String,
    val time: Long = System.currentTimeMillis()
): SocketData("image"), LocalData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (!bytes.contentEquals(other.bytes)) return false
        if (sender != other.sender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + sender.hashCode()
        return result
    }
}

data class LocalImage(
    val uri: Uri
): LocalData