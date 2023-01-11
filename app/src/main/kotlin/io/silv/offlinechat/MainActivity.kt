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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import io.silv.offlinechat.ui.ContentMain
import io.silv.offlinechat.ui.MessageScreen
import io.silv.offlinechat.ui.onboarding.OnboardScreen
import io.silv.offlinechat.ui.theme.OfflineChatTheme
import org.koin.android.ext.android.get
import org.koin.androidx.compose.getViewModel


class MainActivity : ComponentActivity() {

    private val receiver: WifiP2pReceiver = get()
    private val wifiP2pIntentFilter = IntentFilter()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWifiP2pIntents()

        setContent {

            val viewModel : MainActivityViewModel = getViewModel()

            OfflineChatTheme {
                // A surface container using the 'background' color from the theme
                val permissionState = rememberMultiplePermissionsState(permissions = createPermissionsList())
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (permissionState.allPermissionsGranted) {
                        val (s, setS) = mutableStateOf(true)
                        if (viewModel.connectionInfo?.groupFormed != true) {
                            ContentMain(
                                viewModel = viewModel,
                                showToast = {
                                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                                },
                                navigateToMessage = {
                                    setS(false)
                                }
                            )
                        } else {
                            MessageScreen(viewModel = viewModel)
                        }
                    } else {
                       OnboardScreen(permissionState, { })
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
    private fun createPermissionsList() =
        buildList {
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            //location
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            //relative position of devices
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
}

