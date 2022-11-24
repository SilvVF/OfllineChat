package io.silv.offlinechat

import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.silv.offlinechat.ui.PeerListItem
import io.silv.offlinechat.ui.theme.OfflineChatTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.inject
import org.koin.core.parameter.parametersOf


class MainActivity : ComponentActivity() {

    private val channel: WifiP2pManager.Channel = get()
    private val manager: WifiP2pManager = get()
    private val receiver: WifiP2pReceiver = get()
    private val wifiP2pIntentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWifiP2pIntents()

        val viewModel by inject<MainActivityViewModel>()

        setContent {

            val peers by viewModel.peers.collectAsState()

            LaunchedEffect(key1 = true) {
                viewModel.sideEffects.collect {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT ).show()
                }
            }

            OfflineChatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Text(text = "Peers")
                        LazyColumn(Modifier.fillMaxSize(),  verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                            items(peers) {
                                PeerListItem(device = it, modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .wrapContentHeight())
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
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
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OfflineChatTheme {
        Greeting("Android")
    }
}
