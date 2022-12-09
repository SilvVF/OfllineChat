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
import io.silv.offlinechat.wifiP2p.WifiP2pError
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket


class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    private val channel: WifiP2pManager.Channel = receiver.channel,
    private val manager: WifiP2pManager = receiver.manager
): ViewModel() {

    val peers = receiver.peersList
    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()
    private val errorChannel = receiver.mutableErrorChannel.receiveAsFlow()
    val connectionInfo = receiver.connectionInfo.asStateFlow()

    var port by mutableStateOf(0)
    var host by mutableStateOf("")
    init {
        viewModelScope.launch {
            errorChannel.collect {error ->
                when(error) {
                    is WifiP2pError.PeerDiscoveryFailure -> viewModelScope.launch {
                        mutableSideEffectChannel.send("Failed to Discover peers error code ${error.reasonCode}")
                    }
                    WifiP2pError.WifiDirectNotEnabled -> viewModelScope.launch {
                        mutableSideEffectChannel.send("Wifi Direct Not enabled")
                    }
                    is WifiP2pError.DataR -> viewModelScope.launch {
                        mutableSideEffectChannel.send(error.d)
                    }
                }
            }
        }
    }

    fun connect() = viewModelScope.launch {
        connectionInfo.first()?.let { info ->
            when (info.isGroupOwner) {
                true -> {
                    setupServer(port)
                }
                false -> {
                    setupClient(info.groupOwnerAddress.hostAddress ?: "", port)
                }
                else -> Unit
            }
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

    private fun setupClient(host: String, port: Int) =
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {

                    kotlin.runCatching {

                    while (true) {
                        val socket = Socket()
                        socket.bind(null)
                        socket.connect(InetSocketAddress(host, 8888))
                        val outputStream = socket.getOutputStream()
                        outputStream.write("hello".encodeToByteArray())
                        println("sent message from client")
                        outputStream.flush()
                        socket.close()
                    }
                }
            }
        }

    private fun setupServer(port: Int) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.IO) {

            val server = ServerSocket(8888)

            while (true) {
                println("awaiting connection")
                val client = server.accept()
                println("Connected")
                launch {
                    val input = BufferedReader(InputStreamReader(client.getInputStream()))
                    val i = input.readLines().joinToString()
                    println("message $i")
                    input.close()
                    client.close()
                }
            }
        }
    }
}