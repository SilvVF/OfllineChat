@file:OptIn(InternalSerializationApi::class, InternalSerializationApi::class)

package io.silv.offlinechat.data.ktor


import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.*
import io.silv.offlinechat.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.slf4j.event.Level
private suspend fun onReceived(data: SocketData, mutableLocalDataFlow: MutableSharedFlow<LocalData>) {
    when (data) {
        is Message -> {
            Log.d("Received", data.toString())
            mutableLocalDataFlow.emit(data)
        }
        is Ack -> {
            Log.d("Received", data.toString())
            mutableLocalDataFlow.emit(ChatRequest(data.mac, data.name))
        }
        is Image -> {
            Log.d("Received", data.toString())
            mutableLocalDataFlow.emit(data)
        }
    }
}

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

class KtorWebsocketServer {

    private val mutableLocalDataFlow = MutableSharedFlow<LocalData>()

    private var socket: DefaultWebSocketServerSession? = null
    suspend fun subscribeToSocketData(
        callback: suspend (LocalData) -> Unit
    ) {
        mutableLocalDataFlow.asSharedFlow().collect { data ->
            callback(data)
        }
    }

    suspend fun sendAck(ack: Ack) {
        socket?.send(Json.encodeToString(ack))
    }
    suspend fun sendMessage(message: Message): Boolean {
        return runCatching {
            socket?.send(Frame.Text(Json.encodeToString(message)))
        }.isSuccess
    }
    suspend fun sendImage(image: Image): Boolean {
        return runCatching {
            socket?.send(Frame.Text(Json.encodeToString(image)))
        }.isSuccess
    }
    fun start(port: Int, hostAddr: String) = CoroutineScope(Dispatchers.IO).launch {
        embeddedServer(
            factory = Netty,
            port = port,
            host = hostAddr
        ) {

            install(CallLogging) {
                level = Level.INFO
            }
            install(Routing)
            install(WebSockets)



            routing {
                    webSocket("/echo") {
                        socket = this
                        incoming.receiveAsFlow().collect { frame ->
                            if (frame is Frame.Text) {
                                try {
                                    when (val data = parseJsonToSocketData(frame.readText())) {
                                        is Message -> {
                                                Log.d("Received", data.toString())
                                            mutableLocalDataFlow.emit(data)
                                        }
                                        is Ack -> {
                                            Log.d("Received", data.toString())
                                                mutableLocalDataFlow.emit(ChatRequest(data.mac, data.name))
                                            }
                                            is Image -> {
                                                Log.d("Received", data.toString())
                                                mutableLocalDataFlow.emit(data)
                                            }
                                        }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                }
            }
        }.start(true)
    }
}
class KtorWebsocketClient {

    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.websocket.WebSockets)
    }

    private val mutableLocalDataFlow = MutableSharedFlow<LocalData>()

    suspend fun subscribeToSocketData(
        callback: suspend (LocalData) -> Unit
    ) {
        mutableLocalDataFlow.asSharedFlow().collect { data ->
            callback(data)
        }
    }

    private var session: DefaultClientWebSocketSession? = null

    fun connect(
        port: Int,
        hostname: String,
        onSuccess: suspend () -> Unit,
        onFailure: suspend () -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        runCatching {
           client.webSocket(method = HttpMethod.Get, host = hostname, port = port, path = "/echo") {
               session = this
               incoming.receiveAsFlow().collect { frame ->
                   if (frame is Frame.Text) {
                       try {
                           onReceived(
                               data = parseJsonToSocketData(frame.readText()),
                               mutableLocalDataFlow
                           )
                       } catch (e: Exception) {
                           e.printStackTrace()
                       }
                   }
               }
           }
         }.onFailure { exception ->
            exception.printStackTrace()
            onFailure()
         }.onSuccess {
             onSuccess()
        }
    }

    suspend fun sendMessage(message: Message): Boolean {
        return runCatching {
            session?.send(Json.encodeToString(message))
        }.isSuccess
    }

    suspend fun sendImage(image: Image): Boolean {
        return runCatching {
            session?.send(Json.encodeToString(image))
        }.isSuccess
    }
}
