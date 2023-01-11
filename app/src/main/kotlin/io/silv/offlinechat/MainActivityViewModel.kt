package io.silv.offlinechat

import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silv.offlinechat.data.*
import io.silv.offlinechat.data.ktor.KtorWebsocketClient
import io.silv.offlinechat.data.ktor.KtorWebsocketServer
import io.silv.offlinechat.data.room.UriAsStringSerializer
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.WifiP2pError
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable


class MainActivityViewModel(
    private val messageImageRepo: ImageFileRepo,
    private val receiver: WifiP2pReceiver,
    val attachmentReceiver: ImageReceiver,
    private val ktorWebsocketServer: KtorWebsocketServer,
    private val ktorWebsocketClient: KtorWebsocketClient,
    //private val db: DatabaseRepo
): ViewModel() {

    val connectionInfo = receiver.wifiP2pInfo.asStateFlow()

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()
    val peers = receiver.peersList
    var messages by mutableStateOf(emptyList<Chat>())

    var isServer by mutableStateOf<Boolean>(false)
        private set
    init {
        listenForConnection()
        collectReceiverErrors()
        viewModelScope.launch {
            messages.asFlow().collect {
                println(it)
            }
        }
    }

    private fun listenForConnection() = viewModelScope.launch {
        connectionInfo.collectLatest {
            it?.let { info ->
                val groupOwnerAddress = info.groupOwnerAddress?.hostAddress ?: return@collectLatest
                if (info.isGroupOwner) {
                    isServer = true
                    ktorWebsocketServer.start(8888, groupOwnerAddress)
                    ktorWebsocketServer.subscribeToSocketData { data ->
                        Log.d("Received", data.toString())
                        receivedLocalData(data)
                    }
                } else {
                    isServer = false
                    delay(2000)
                    runCatching {
                        ktorWebsocketClient.connect(
                            port = 8888,
                            hostname = groupOwnerAddress,
                            mac = receiver.getDeviceMacAddress()
                        )
                    }
                    ktorWebsocketClient.subscribeToSocketData { data ->
                        Log.d("Received", data.toString())
                        receivedLocalData(data)
                    }
                }
            }
        }
    }

    private suspend fun receivedLocalData(localData: LocalData) {
        when(localData) {
            is Message -> {
                messages = buildList {
                    add(Chat.ReceivedMessage(localData.content, System.currentTimeMillis()))
                    addAll(messages)
                }
            }
            is Image -> {
                viewModelScope.launch {
                    writeBytesToFileRepo(messageImageRepo, localData).collect { uri ->
                        messages = buildList {
                            add(Chat.ReceivedImage(uri, System.currentTimeMillis()))
                            addAll(messages)
                        }
                    }
                }
            }
            is ChatRequest ->  {
                if (isServer) {
                    ktorWebsocketServer.sendAck(
                        Ack(mac = receiver.getDeviceMacAddress(), name = "server hello")
                    )
                }
            }
        }
    }

    fun sendMessageUsingKtor(message: String) = viewModelScope.launch {
        if (isServer) {
            ktorWebsocketServer.sendMessage(Message(message, "server"))
        } else {
            ktorWebsocketClient.sendMessage(Message(message, "client"))
        }
        messages = buildList {
            add(Chat.SentMessage(message, System.currentTimeMillis()))
            addAll(messages)
        }
        attachmentReceiver.getLocalUrisForSend(
            onCompletion = { attachmentReceiver.clearImages() }
        ) { attachmentUris ->
            launch {
                attachmentUris.forEach { aUri ->
                    launch {
                        val time = System.currentTimeMillis()
                        val (file, uri) = messageImageRepo.write(aUri)
                        val image = Image(
                            bytes = file.readBytes(),
                            sender = "sender", time = time
                        )
                        if (isServer) {
                            ktorWebsocketServer.sendImage(image)
                        } else  {
                            ktorWebsocketClient.sendImage(image)
                        }
                        messages = buildList {
                            add(Chat.SentImage(uri, time))
                            addAll(messages)
                        }
                    }
                }
            }.join()
        }
    }

    fun connectToDevice(device: WifiP2pDevice) {
        viewModelScope.launch {
            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }
            receiver.channel.also { channel ->
                receiver.manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                        Log.d("peers", "onSuccess called connectTodevice()")
                    }

                    override fun onFailure(reason: Int) {
                        //failure logic
                        Log.d("peers", "onFailure called connectToDevice()")
                    }
                })
            }
        }
    }

    private fun collectReceiverErrors() = viewModelScope.launch {
        receiver.errorChannel.collect {
            when (it) {
                is WifiP2pError.PeerDiscoveryFailure ->
                    mutableSideEffectChannel.send(
                        "Failed to Discover peers error code ${it.reasonCode}"
                    )
                is WifiP2pError.WifiDirectNotEnabled -> mutableSideEffectChannel.send("Wifi Direct Not enabled")
                is WifiP2pError.DataR -> mutableSideEffectChannel.send(it.d)
            }
        }
    }
}
sealed interface Chat {
    @Serializable
    data class SentMessage(val s: String, val time: Long): Chat
    @Serializable
    data class ReceivedMessage(val s: String, val time: Long): Chat
    @Serializable
    data class SentImage(
        @Serializable(UriAsStringSerializer::class) val uri: Uri,
        val time: Long
    ): Chat
    @Serializable
    data class ReceivedImage(
        @Serializable(UriAsStringSerializer::class) val uri: Uri,
        val time: Long
    ): Chat
}
