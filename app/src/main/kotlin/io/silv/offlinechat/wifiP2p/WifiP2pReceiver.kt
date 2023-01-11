package io.silv.offlinechat.wifiP2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class P2pEvent {
    data class StateChanged(val enabled: Boolean): P2pEvent()

    data class PeersChanged(val peers: List<WifiP2pDevice>): P2pEvent()

    data class ConnectionChanged(val networkInfo: NetworkInfo?, val p2pInfo: WifiP2pInfo?): P2pEvent()

    data class DeviceChanged(val device: WifiP2pDevice?): P2pEvent()

    data class Error(val message: String, val throwable: Throwable? = null): P2pEvent()
}
class WifiP2pReceiver(
    val manager: WifiP2pManager,
    val channel: WifiP2pManager.Channel,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): BroadcastReceiver() {

    val wifiP2pInfo = MutableStateFlow<WifiP2pInfo?>(null)

    private val mutableEventFlow = MutableSharedFlow<P2pEvent>()
    val eventReceiveFlow = mutableEventFlow.asSharedFlow()
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("peers", "onReceive invoked data ${intent.data.toString()}")
        scope.launch {
            when(intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    Log.d("WifiP2pReceiver", "WIFI_P2P_STATE_CHANGED_ACTION")
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    mutableEventFlow.emit(
                        P2pEvent.StateChanged(
                            enabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        )
                    )
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d("WifiP2pReceiver", "WIFI_P2P_PEERS_CHANGED_ACTION")
                    val deviceList = intent.getExtra<WifiP2pDeviceList>(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                    mutableEventFlow.emit(
                        P2pEvent.PeersChanged(deviceList?.deviceList?.toList() ?: emptyList())
                    )
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d("WifiP2pReceiver", "WIFI_P2P_CONNECTION_CHANGED_ACTION ")
                    val networkInfo = intent.getExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    val p2pInfo = intent.getExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                    mutableEventFlow.emit(
                        P2pEvent.ConnectionChanged(networkInfo, p2pInfo)
                    )
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    Log.d("WifiP2pReceiver", "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")
                    val device = intent.getExtra<WifiP2pDevice>(EXTRA_WIFI_P2P_DEVICE)
                    mutableEventFlow.emit(
                        P2pEvent.DeviceChanged(device)
                    )
                }
            }
        }
    }
    suspend fun connectToDevice(
        device: WifiP2pDevice,
        onSuccess: () -> Unit = { },
        onFailure: (code: Int) -> Unit = { }
    ) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                Log.d("peers", "onSuccess called connectTodevice()")
                onSuccess()
            }
            override fun onFailure(reason: Int) {
                //failure logic
                Log.d("peers", "onFailure called connectToDevice()")
                onFailure(reason)
            }
        })
    }
    suspend fun refreshPeers(
        onFailure: (code: Int) -> Unit = { },
        onSuccess: suspend () -> Unit = { },
    ) {
        Log.d("WifiP2pReceiver", "discoverPeers() invoked")
        manager.discoverPeers(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("WifiP2pReceiver", "discoverPeers() success")
                    scope.launch(Dispatchers.Default) {
                        onSuccess()
                    }
                }
                override fun onFailure(reasonCode: Int) {
                    onFailure(reasonCode)
                }
            }
        )
    }
}

private inline fun <reified T>Intent.getExtra(extra: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    this.getParcelableExtra(extra, T::class.java)
else this.getParcelableExtra(extra) as? T?