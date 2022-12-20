package io.silv.offlinechat.data

import kotlinx.serialization.Serializable

@Serializable
abstract class SocketData(val type: String)

@Serializable
class Ack: SocketData("ack")

@Serializable
data class Message(
    val content: String,
    val sender: String,
) : SocketData("message")

@Serializable
data class Image(
    val uri: ByteArray,
    val sender: String
): SocketData("image") {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (!uri.contentEquals(other.uri)) return false
        if (sender != other.sender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.contentHashCode()
        result = 31 * result + sender.hashCode()
        return result
    }
}