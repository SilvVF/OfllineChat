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