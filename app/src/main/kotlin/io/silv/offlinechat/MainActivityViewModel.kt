package io.silv.offlinechat

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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

    init {
        receiver.connectionListenerCallback = {
            if (it.isGroupOwner)
                setupServer(it)
            else
                setupClient(it)
        }
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

    private fun setupServer(wifiP2pInfo: WifiP2pInfo) {
        viewModelScope.launch(Dispatchers.IO) {
        val message = """
            setupServer invoked:
                    + group formed ${wifiP2pInfo.groupFormed} 
                    + group owner address ${wifiP2pInfo.groupOwnerAddress}
                    + is group owner ${wifiP2pInfo.isGroupOwner}
             """.trimIndent()
            Log.d("peers", message)
            mutableSideEffectChannel.send(message)
            runCatching {
                DeviceServer(wifiP2pInfo).run()
            }.onFailure {
                Log.d("peers" , "server error : ${it.message}")
            }
        }
    }

    private fun setupClient(wifiP2pInfo: WifiP2pInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = """
            setupClient invoked:
                    + group formed ${wifiP2pInfo.groupFormed} 
                    + group owner address ${wifiP2pInfo.groupOwnerAddress}
                    + is group owner ${wifiP2pInfo.isGroupOwner}
             """.trimIndent()
            Log.d("peers", message)
            mutableSideEffectChannel.send(message)
            runCatching {
                DeviceClient(wifiP2pInfo).run()
            }.onFailure {
                Log.d("peers" , "client error : ${it.message}")
            }
        }
    }

}