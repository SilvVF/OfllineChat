package io.silv.offlinechat

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val receiver: WifiP2pReceiver
): ViewModel() {

    private val mutablePeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers = mutablePeers.asStateFlow()

    private val mutableChannel = Channel<String>()
    val sideEffects = mutableChannel.receiveAsFlow()

    init {
        receiver.peerListCallback = { peers ->
            peersListCallback(peers)
        }
        receiver.handlePeerDiscoveryFailure = { code: Int ->
            Log.d("peers", "handlePeerDiscoveryFailure invoked data $code")
            viewModelScope.launch {
                mutableChannel.send("failed to discover peers code #$code")
            }
        }
        receiver.handleWifiDirectNotEnabled = {
            Log.d("peers", "handleWifiDirectNotEnabled invoked")
            viewModelScope.launch {
                mutableChannel.send("wifi direct not enabled")
            }
        }
        receiver.discoverPeers()
    }

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->

        // String from WifiP2pInfo struct
        val groupOwnerAddress: String =
            info.groupOwnerAddress.hostAddress ?: return@ConnectionInfoListener

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The other device acts as the peer (client). In this case,
            // you'll want to create a peer thread that connects
            // to the group owner.
        }
    }

    private fun peersListCallback(peers: List<WifiP2pDevice>) = viewModelScope.launch {
        mutablePeers.emit(peers)
        mutableChannel.send("refreshed peers")
        Log.d("peers", "Peers List Callback invoked data $peers")
//
//        val device = peers[0]
//
//        val config = WifiP2pConfig().apply {
//            deviceAddress = device.deviceAddress
//            wps.setup = WpsInfo.PBC
//        }
//
//        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
//            override fun onSuccess() {
//                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
//            }
//
//            override fun onFailure(reasonCode: Int) {
//
//            }
//        })
    }
}