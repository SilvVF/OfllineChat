package io.silv.offlinechat

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    private val channel: WifiP2pManager.Channel,
    private val manager: WifiP2pManager
): ViewModel() {

    val peers = receiver.peersList
    private val mutableChannel = Channel<String>()
    val sideEffects = mutableChannel.receiveAsFlow()
    private val errorChannel = receiver.mutableErrorChannel.receiveAsFlow()
    val connectionInfo = receiver.connectionInfo.asStateFlow()

    init {
        viewModelScope.launch {
            errorChannel.collect {error ->
                when(error) {
                    is WifiP2pError.PeerDiscoveryFailure -> viewModelScope.launch {
                        mutableChannel.send("Failed to Discover peers error code ${error.reasonCode}")
                    }
                    WifiP2pError.WifiDirectNotEnabled -> viewModelScope.launch {
                        mutableChannel.send("Wifi Direct Not enabled")
                    }
                    is WifiP2pError.DataR -> viewModelScope.launch {
                        mutableChannel.send(error.d)
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
                        Log.d("peers", "onSuccess called Connect to device")
                    }
                    override fun onFailure(reason: Int) {
                        //failure logic
                        Log.d("peers", "onFailure called Connect to device")
                    }
                })
            }
        }
    }

}