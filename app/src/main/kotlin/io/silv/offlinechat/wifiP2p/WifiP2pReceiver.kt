package io.silv.offlinechat.wifiP2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class WifiP2pReceiver(
    val manager: WifiP2pManager,
    val channel: WifiP2pManager.Channel,
    var scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): BroadcastReceiver() {

    private val mutablePeersList = MutableStateFlow(emptyList<WifiP2pDevice>())
    val peersList = mutablePeersList.asStateFlow()

    private val mutableErrorChannel = Channel<WifiP2pError>()
    val errorChannel = mutableErrorChannel.receiveAsFlow()

    val connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("peers", "onReceive invoked data ${intent.data.toString()}")
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                Log.d("WifiP2pReceiver", "WIFI_P2P_STATE_CHANGED_ACTION")
                // Determine if Wi-Fi Direct mode is enabled or not, alert
                // the Activity.
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                if (!isEnabled) {
                    scope.launch {
                        mutableErrorChannel.send(WifiP2pError.WifiDirectNotEnabled)
                    }
                } else {
                    discoverPeers()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d("WifiP2pReceiver", "WIFI_P2P_PEERS_CHANGED_ACTION")
                manager.requestPeers(channel) { peerList ->
                    scope.launch {
                        mutablePeersList.emit(peerList.deviceList.toList())
                    }
                }

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Log.d("WifiP2pReceiver", "WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                manager.let { manager ->
                    val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel) { info ->
                            scope.launch {
                                connectionInfo.emit(info)
                            }
                        }
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.d("WifiP2pReceiver", "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
            }
        }
    }
    /**
     *  this only initiates peer discovery. starts the discovery process and then immediately returns.
     *  The system notifies you if the peer discovery process is successfully
     *  initiated by calling methods in the provided action listener.
     *  Also, discovery remains active until a connection is initiated or a P2P group is formed.
     */
    private fun discoverPeers() {
        Log.d("WifiP2pReceiver", "discoverPeers() invoked")
        manager.discoverPeers(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WifiP2pReceiver", "discoverPeers() success")
                }

                override fun onFailure(reasonCode: Int) {
                    scope.launch {
                        mutableErrorChannel.send(WifiP2pError.PeerDiscoveryFailure(reasonCode))
                    }
                }
            }
        )
    }
}

sealed class WifiP2pError {
    data class PeerDiscoveryFailure(val reasonCode: Int): WifiP2pError()
    object WifiDirectNotEnabled: WifiP2pError()

    data class DataR(val d: String): WifiP2pError()
}