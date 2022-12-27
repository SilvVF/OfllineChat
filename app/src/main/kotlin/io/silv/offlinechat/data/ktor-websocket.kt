package io.silv.offlinechat.data


import androidx.core.net.toUri
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.io.File
import java.util.*

class KtorWebsocketServer(
    private val imageFileRepo: ImageFileRepo
) {

    private val mutableLocalDataFlow = MutableSharedFlow<LocalData>()

    suspend fun subscribeToSocketData(
        callback: (LocalData) -> Unit
    ) {
        mutableLocalDataFlow.asSharedFlow().collect { data ->
            callback(data)
        }
    }

    fun start(port: Int, hostAddr: String) = CoroutineScope(Dispatchers.IO).launch {
        embeddedServer(
            factory = Netty,
            port, hostAddr
        ) {

            install(Routing)
            install(WebSockets)

            routing {
                route("/") {
                    webSocket {
                        incoming.receiveAsFlow().collect { frame ->
                            if (frame is Frame.Text) {
                                try {
                                    onReceived(
                                        data = parseJsonToSocketData(frame.readText())
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }.start()
    }


    private suspend fun onReceived(data: SocketData) {
        when (data) {
            is Message -> {
                mutableLocalDataFlow.emit(data)
            }
            is Ack -> {

            }
            is Image -> {
                withContext(Dispatchers.IO) {
                    val file = File.createTempFile("temp-image", "${UUID.randomUUID()}")
                    file.writeBytes(data.bytes)
                    val uri = imageFileRepo.write(file.toUri()).second
                    mutableLocalDataFlow.emit(LocalImage(uri))
                    launch { file.delete() }
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
}