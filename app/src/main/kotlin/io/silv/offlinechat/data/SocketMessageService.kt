package io.silv.offlinechat.data

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

var clientAddress by mutableStateOf<InetAddress?>(null)

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: SocketData> sendMessageUsingSocket (
    message: T,
    connectionInfo: WifiP2pInfo,
) = withContext(Dispatchers.IO) {
        runCatching {
                val socket = Socket()
                socket.bind(null)

                val (serverAddress, serverPort) = if (connectionInfo.isGroupOwner){
                    clientAddress to 8848
                } else {
                    connectionInfo.groupOwnerAddress to 8888
                }

                socket.connect(
                    InetSocketAddress(
                        serverAddress,
                        serverPort,
                    )
                )
                val outputStream = socket.getOutputStream()
                outputStream.write(
                    Json.encodeToString(T::class.serializer(), message).logJson()
                        .encodeToByteArray()
                )
                println("sent message from client")
                outputStream.flush()
                socket.close()
        }
}

fun setupServer(
    port: Int = 8888,
    onReceived: suspend (String) -> Unit = { }
) = CoroutineScope(Dispatchers.IO).launch {
    withContext(Dispatchers.IO) {

        val server = ServerSocket(port)


        while (true) {
            println("awaiting connection")
            val client = server.accept()

            println("Connected")
            launch(Dispatchers.IO) {
                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val text = input.readText()
                runCatching {
                    when(val socketData = parseJsonToSocketData(text)) {
                        is Ack -> clientAddress = client.inetAddress
                        is Message -> {
                            onReceived(socketData.content)
                        }
                    }
                }.onFailure {
                    // Received Bad message
                }
                input.close()
                client.close()
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
private fun parseJsonToSocketData(json: String): SocketData {
        val type = Json.parseToJsonElement(json).jsonObject["type"]
            ?.toString()
            ?.trim()
            ?.removeSurrounding("\"")
    repeat(1) {
        println(json)
        println(type)
    }
        val serializer = when(type) {
            "message" -> Message::class.serializer()
            "ack" -> return Ack()
            else -> throw SerializationException("type in json does not conform to socket object types")
        }
        return Json.decodeFromString(serializer, json)
}

fun String.logJson() = this.also { Log.d("json", this.trimIndent()) }
