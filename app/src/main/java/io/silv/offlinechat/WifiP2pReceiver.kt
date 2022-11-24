package io.silv.offlinechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log


class WifiP2pReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    var handleWifiDirectNotEnabled: (() -> Unit)? = null,
    var handlePeerDiscoveryFailure: ((code: Int) -> Unit)? = null,
    var peerListCallback: ((peers: List<WifiP2pDevice>) -> Unit)? = null
): BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        Log.d("peers", "onReceive invoked data ${intent.data.toString()}")
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                Log.d("peers", "WIFI_P2P_STATE_CHANGED_ACTION")
                // Determine if Wi-Fi Direct mode is enabled or not, alert
                // the Activity.
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                if (!isEnabled) {
                    handleWifiDirectNotEnabled?.invoke()
                } else {
                    discoverPeers()
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d("peers", "WIFI_P2P_PEERS_CHANGED_ACTION")
                val peers = mutableListOf<WifiP2pDevice>()
                // The peer list has changed! We should probably do something about
                // that.
                manager.requestPeers(channel, WifiP2pManager.PeerListListener { peerList ->
                    val refreshedPeers = peerList.deviceList
                    if (refreshedPeers != peers) {
                        peers.clear()
                        peers.addAll(refreshedPeers)
                        // Perform any other updates needed based on the new list of
                        // peers connected to the Wi-Fi P2P network.
                    }
                    peerListCallback?.invoke(peers)
                })

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Log.d("peers", "WIFI_P2P_CONNECTION_CHANGED_ACTION ")

                // Connection state changed! We should probably do something about
                // that.

            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.d("peers", "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
            }
        }
    }





    /**
     *  this only initiates peer discovery. starts the discovery process and then immediately returns.
     *  The system notifies you if the peer discovery process is successfully
     *  initiated by calling methods in the provided action listener.
     *  Also, discovery remains active until a connection is initiated or a P2P group is formed.
     */
    fun discoverPeers() {
        Log.d("peers", "discover peers")
        manager.discoverPeers(
            /*channel =*/channel,
            /*actionListener =*/object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("peers", "discover peers success")
                }

                override fun onFailure(reasonCode: Int) {
                    handlePeerDiscoveryFailure?.invoke(reasonCode)
                }
            }
        )
    }
}