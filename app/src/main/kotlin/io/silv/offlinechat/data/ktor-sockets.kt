package io.silv.offlinechat.data

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration

class  KtorSocketsServer {

    private var receiveChannel: ByteReadChannel? = null
    private var sendChannel: ByteWriteChannel? = null

     fun startServer(port: Int, hostname: String) = CoroutineScope(Dispatchers.IO).launch {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(hostname, port)
        println("Server is listening at ${serverSocket.localAddress}")

        while (true) {
            val socket = serverSocket.accept()
            receiveChannel = socket.openReadChannel()
            sendChannel = socket.openWriteChannel(autoFlush = true)
            println("Accepted ${socket.localAddress} ${socket.remoteAddress}  $socket")
            Log.d("Received message in KTOR", "$socket")
            launch {
                try {
                    while (true) {
                        sendChannel?.writeStringUtf8("hello from server")
                        receiveChannel?.readUTF8Line()?.let { data ->
                            println(data)
                            when(val socketData = parseJsonToSocketData(data)) {
                                else -> Log.d("Received message in KTOR", socketData.toString())
                            }
                        }

                    }
                } catch (e: Throwable) {
                    socket.close()
                }
            }
        }
    }

    suspend fun sendMesssage(s: String) = withContext(Dispatchers.IO){
        sendChannel?.writeStringUtf8(Json.encodeToString(Message.serializer(), Message("dfkajsfaklsdfjklsafj", "dkfksljf")))
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

class KtorSocketsClient {

    private val socket = aSocket(SelectorManager(Dispatchers.IO)).tcp()
    private var receiveChannel: ByteReadChannel? = null
    private var sendChannel: ByteWriteChannel? = null

    fun startClient(port: Int, hostname: String) = CoroutineScope(Dispatchers.IO).launch {
        kotlinx.coroutines.delay(3000L)
        val socketConnection = socket.connect(hostname, port).also {
            Log.d("KtorSocketsClient", "${it.remoteAddress} ${it.localAddress} ")
        }
        receiveChannel = socketConnection.openReadChannel()
        sendChannel = socketConnection.openWriteChannel(autoFlush = true)

        launch {
            while (true) {
                try {
                    sendChannel?.writeStringUtf8("Hello From Client")
                    val data = receiveChannel?.readUTF8Line()?.let {
                        Log.d("KtorSocketsClient", it)
                    }
                } catch (e: Exception) {
                    println("Error")
                }
            }
        }
    }

    suspend fun writeToSendChannel(s: String) {
        sendChannel?.writeStringUtf8(s)
    }

    @OptIn(InternalSerializationApi::class)
    suspend inline fun <reified T: SocketData> sendSocketDataOverSocket (
        message: T,
    ) = withContext(Dispatchers.IO) {
       //sendChannel?.writeFully(Json.encodeToString(Message.serializer(), Message("client sdkfjlaksf", "dkfaslkfjklasf")).encodeToByteArray())
//        runCatching {
//            writeToSendChannel(Json.encodeToString(T::class.serializer(), message).logJson())
//            println("sent message from client ktor")
//        }
    }
}