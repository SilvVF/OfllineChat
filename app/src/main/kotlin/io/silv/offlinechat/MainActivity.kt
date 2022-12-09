package io.silv.offlinechat

import android.Manifest
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import io.silv.offlinechat.data.globalIp
import io.silv.offlinechat.data.globalPort
import io.silv.offlinechat.ui.ContentMain
import io.silv.offlinechat.ui.PermissionRequestScreen
import io.silv.offlinechat.ui.theme.OfflineChatTheme
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject


class MainActivity : ComponentActivity() {

    private val receiver: WifiP2pReceiver = get()
    private val wifiP2pIntentFilter = IntentFilter()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWifiP2pIntents()

        val viewModel by inject<MainActivityViewModel>()


        setContent {

            OfflineChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Text(text = globalIp)
                    Text(text = globalPort)
                    val permissionState = rememberMultiplePermissionsState(permissionsList())
                    if (permissionState.allPermissionsGranted) {
                        ContentMain(
                            viewModel = viewModel,
                            showToast = {
                                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                       PermissionRequestScreen(permissionsState = permissionState)
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        receiver.also { receiver ->
            registerReceiver(receiver, wifiP2pIntentFilter)
        }
    }
    override fun onPause() {
        super.onPause()
        receiver.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    private fun setWifiP2pIntents() {
        wifiP2pIntentFilter.apply {
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }
    private fun permissionsList() =
        buildList {
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toList()
}

