package io.silv.offlinechat

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
import io.silv.offlinechat.wifiP2p.WifiP2pError
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*


class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    private val channel: WifiP2pManager.Channel = receiver.channel,
    private val manager: WifiP2pManager = receiver.manager,
): ViewModel() {

    val peers = receiver.peersList
    val connectionInfo = receiver.connectionInfo.asStateFlow()

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()


    var messages by mutableStateOf(emptyList<String>())


    init {
        listenForConnection()
        collectReceiverErrors()
    }

    private fun listenForConnection() = viewModelScope.launch {
        connectionInfo.collect { wifiInfo ->
            wifiInfo?.let {
                if (wifiInfo.isGroupOwner) {
                    setupServer { socketData ->
                        messages = listOf(socketData) + messages
                    }
                } else  {
                    setupServer(port = 8848) { socketData ->
                        messages = listOf(socketData) + messages
                    }
                    repeat(5) {
                        sendMessageUsingSocket(
                            message = Ack(), wifiInfo,
                        ).onFailure {
                            mutableSideEffectChannel.send(it.message ?: "")
                        }
                    }
                }
            }
        }
    }

    fun sendMessageFromClient(m: String) = viewModelScope.launch {
        sendMessageUsingSocket(
            message = Message(
                sender = "name",
                content = m
            ),
            connectionInfo.value ?: return@launch
        ).onSuccess {
            messages = listOf(m) + messages
        }.onFailure { error ->
            mutableSideEffectChannel.send(error.message ?: "error")
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