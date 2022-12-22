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
import java.net.ServerSocket

private var connectedAlready = false
private var serverJob: Job? = null

class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    private val channel: WifiP2pManager.Channel = receiver.channel,
    private val manager: WifiP2pManager = receiver.manager,
    val imageReceiver: ImageReceiver,
    private val imageRepoForMessages: ImageFileRepo,
): ViewModel() {

    val peers = receiver.peersList
    val connectionInfo = receiver.connectionInfo.asStateFlow()

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()



    var messages by mutableStateOf(emptyList<Chat>())

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
        connectionInfo.collect { wifiInfo ->
            if (!connectedAlready) {
                wifiInfo?.let {
                    if (wifiInfo.isGroupOwner) {
                        connectedAlready = true
                        serverJob = setupServer(
                            imageRepoForMessages,
                            ServerSocket(8888),
                            onImageReceived =  { imageUri, time ->
                                onImage(imageUri, time)
                            }
                        ) { socketData, time ->
                            messages = listOf(Chat.Message(socketData, time)) + messages
                        }
                        serverJob?.start()
                    } else {
                        connectedAlready = true
                        serverJob = setupServer(imageRepoForMessages,ServerSocket(8848),
                           onImageReceived =  { imageUri, time ->
                               onImage(imageUri, time)
                           }
                        ) { socketData, time ->
                            messages = listOf(Chat.Message(socketData, time)) + messages
                        }
                        serverJob?.start()

                        repeat(5) {
                            sendSocketDataOverSocket(
                                message = Ack(), wifiInfo,
                            ).onFailure {
                                mutableSideEffectChannel.send(it.message ?: "")
                            }
                        }
                    }
                } ?: run {
                    connectedAlready = false
                    serverJob?.cancel()
                    serverJob = null
                    imageRepoForMessages.deleteAll()
                }
            }
        }
    }

    private fun onImage(uri: Uri, time: Long) {
        println("onImage $uri")
        messages = (listOf(Chat.Image(uri, time)) + messages).sortedByDescending { it.t }
    }

    fun sendMessageFromClient(m: String) = viewModelScope.launch {
        connectionInfo.value?.let {info ->
            imageReceiver.uriFlow.firstOrNull()?.let{ uriList ->
                imageReceiver.lockRepoForOp { locked -> if (!locked) { return@lockRepoForOp }
                    uriList.forEach { localUri ->
                        launch {
                            val (file, newUri) = imageRepoForMessages.write(localUri)
                            val currTime = System.currentTimeMillis()
                            sendSocketDataOverSocket(
                                message = Image(
                                    uri =  file.readBytes(),
                                    sender = "name",
                                    time = currTime
                                ),
                                info
                            ).onSuccess {
                                messages = (listOf(Chat.Image(newUri, currTime)) + messages).sortedByDescending { it.t }
                            }
                        }
                    }
                }
            }
            sendSocketDataOverSocket(
                message = Message(
                    sender = "name",
                    content = m
                ),
                info
            ).onSuccess {
                messages = (listOf(Chat.Message(m, System.currentTimeMillis())) + messages).sortedByDescending { it.t }
            }.onFailure { error ->
                mutableSideEffectChannel.send(error.message ?: "error")
            }
        }
    }.invokeOnCompletion {
        viewModelScope.launch {
            imageReceiver.clearImages()
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
                    mutableSideEffectChannel.send("Failed to Discover peers error code ${it.reasonCode}")
                is WifiP2pError.WifiDirectNotEnabled -> mutableSideEffectChannel.send("Wifi Direct Not enabled")
                is WifiP2pError.DataR -> mutableSideEffectChannel.send(it.d)
            }
        }
    }
}

sealed class Chat(val t: Long) {
    data class Message(val s: String, val time: Long): Chat(time)
    data class Image(val uri: Uri, val time: Long): Chat(time)
}