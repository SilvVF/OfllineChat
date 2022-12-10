package io.silv.offlinechat.data

@kotlinx.serialization.Serializable
abstract class SocketData(val type: String)

@kotlinx.serialization.Serializable
class Ack: SocketData("ack")

@kotlinx.serialization.Serializable
data class Message(
    val content: String,
    val sender: String,
) : SocketData("message")