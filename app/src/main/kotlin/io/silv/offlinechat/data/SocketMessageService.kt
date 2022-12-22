package io.silv.offlinechat.data

import android.net.Uri
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

var clientAddress by mutableStateOf<InetAddress?>(null)

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: SocketData> sendSocketDataOverSocket (
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
    imageRepo: ImageFileRepo,
    server: ServerSocket,
    onImageReceived: suspend (Uri, Long) -> Unit,
    onReceived: suspend (String, Long) -> Unit
) = CoroutineScope(Dispatchers.IO).launch {

        while (true) {

            println("awaiting connection")
            val client = server.accept()

            println("Connected")
            launch(Dispatchers.IO) {
                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val text = input.readText()
                input.close()
                client.close()
                runCatching {
                    when(val socketData = parseJsonToSocketData(text)) {
                        is Ack -> clientAddress = client.inetAddress
                        is Message -> {
                            onReceived(socketData.content, socketData.time)
                        }
                        is Image -> {
                            val file = File.createTempFile("temp-image", "${UUID.randomUUID()}")
                            file.writeBytes(socketData.uri)
                            val uri = imageRepo.write(
                                file.toUri()
                            ).second
                            launch { file.delete() }
                            onImageReceived(uri, socketData.time)
                        }
                    }
                }.onFailure {
                    // Received Bad message
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
            "image" -> Image::class.serializer()
            else -> throw SerializationException("type in json does not conform to socket object types")
        }
        return Json.decodeFromString(serializer, json)
}

fun String.logJson() = this.also { Log.d("json", this.trimIndent()) }
