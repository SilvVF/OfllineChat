package io.silv.offlinechat.repositories

import android.util.Log
import io.silv.offlinechat.Chat
import io.silv.offlinechat.data.*
import io.silv.offlinechat.data.ktor.KtorWebsocketClient
import io.silv.offlinechat.data.ktor.KtorWebsocketServer
import io.silv.offlinechat.ui.ImageReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.withLock

class MessageRepo(
    private val messageImageRepo: ImageFileRepo,
    private val ktorWebsocketServer: KtorWebsocketServer,
    private val ktorWebsocketClient: KtorWebsocketClient,
) {


    suspend fun startListeningForMessages(
        isGroupOwner: Boolean,
        groupOwnerAddress: String,
        onReceived: suspend (data: LocalData) -> Unit
    ) {
        if (isGroupOwner) {
            ktorWebsocketServer.start(8888, groupOwnerAddress)
            ktorWebsocketServer.subscribeToSocketData { data ->
                Log.d("Received Repo", data.toString())
                when (data) {
                    is Image -> {
                        writeBytesToFileRepo(messageImageRepo, data).collect { uri ->
                            onReceived(LocalImage(uri, data.time))
                        }
                    }
                    else -> onReceived(data)
                }
            }
        } else {
            delay(3000)
            ktorWebsocketClient.connect(
                8888,
                groupOwnerAddress,
                onSuccess = {
                    ktorWebsocketClient.subscribeToSocketData { data ->
                        Log.d("Received Repo", data.toString())
                        when (data) {
                            is Image -> {
                                writeBytesToFileRepo(messageImageRepo, data).collect { uri ->
                                    onReceived(LocalImage(uri, data.time))
                                }
                            }
                            else -> onReceived(data)
                        }
                    }
                },
                onFailure = {

                }
            )
        }
    }

    suspend fun sendMessage(
        message: String,
        server: Boolean = false,
        attachmentReceiver: ImageReceiver,
        onFailure: suspend () -> Unit = { },
        onSuccess: suspend (List<Chat>) -> Unit = { },
    ) {
        val successFullySentList = mutableListOf<Chat>()
        val messageToSend = Message(message, "name")
        val result = if (server) {
            ktorWebsocketServer.sendMessage(messageToSend)
        } else {
            ktorWebsocketClient.sendMessage(messageToSend)
        }
        if (result) { successFullySentList.add(
            Chat.SentMessage(message, messageToSend.time)
        ) } else { onFailure() }
        attachmentReceiver.getLocalUrisForSend(
            onCompletion = { attachmentReceiver.clearImages() }
        ) { attachmentUris ->
                attachmentUris.forEach { aUri ->
                    coroutineScope {
                        val time = System.currentTimeMillis()
                        val (file, uri) = messageImageRepo.write(aUri)
                        val image = Image(
                            bytes = file.readBytes(),
                            sender = "sender", time = time
                        )
                        val res = if (server) {
                            ktorWebsocketServer.sendImage(image)
                        } else  {
                            ktorWebsocketClient.sendImage(image)
                        }
                        if (res) {
                            successFullySentList.add(Chat.SentImage(uri, time))
                        }
                    }
                }
        }
        onSuccess(successFullySentList.toList())
    }
}