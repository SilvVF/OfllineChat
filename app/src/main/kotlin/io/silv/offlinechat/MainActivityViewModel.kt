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
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.WifiP2pError
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    val imageReceiver: ImageReceiver,
    private val imageRepoForMessages: ImageFileRepo,
    private val ktorWebsocketServer: KtorWebsocketServer,
    private val ktorWebsocketClient: KtorWebsocketClient
): ViewModel() {

    private val channel = receiver.channel
    private val manager = receiver.manager
    val peers = receiver.peersList
    val connectionInfo = receiver.connectionInfo.asStateFlow()

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()


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
        connectionInfo.collectLatest { wifiInfo ->
            wifiInfo?.let { info ->
                val groupOwnerAddress = info.groupOwnerAddress.hostAddress ?: "127.0.0.1"
                println(info.groupOwnerAddress.hostAddress)
                if (wifiInfo.isGroupOwner) {
                    isServer = true
                    ktorWebsocketServer.start(8888, groupOwnerAddress)
                    ktorWebsocketServer.subscribeToSocketData {
                        receivedLocalData(it)
                    }
                } else {
                    isServer = false
                    delay(2000)
                    launch {  ktorWebsocketClient.connect(8888, groupOwnerAddress) }
                    ktorWebsocketClient.subscribeToSocketData {
                        Log.d("Received", it.toString())
                        receivedLocalData(it)
                    }
                }
            }
        }
    }

    private fun receivedLocalData(localData: LocalData) {
        when(localData) {
            is Message -> {
                messages = buildList {
                    add(Chat.Message(localData.content, System.currentTimeMillis()))
                    addAll(messages)
                }
            }
            is LocalImage -> {
                messages = buildList {
                    add(Chat.Image(localData.uri, System.currentTimeMillis()))
                    addAll(messages)
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
            add(Chat.Message(message, System.currentTimeMillis()))
            addAll(messages)
        }
        imageReceiver.getLocalUrisForSend(
            onCompletion = { imageReceiver.clearImages() }
        ) { attachmentUris ->
            launch {
                attachmentUris.forEach { aUri ->
                    launch {
                        val time = System.currentTimeMillis()
                        val (file, uri) = imageRepoForMessages.write(aUri)
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
                            add(Chat.Image(uri, time))
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
            channel.also { channel ->
                manager.connect(channel, config, object : WifiP2pManager.ActionListener {
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
sealed class Chat {
    data class Message(val s: String, val time: Long): Chat()
    data class Image(val uri: Uri, val time: Long): Chat()
}
