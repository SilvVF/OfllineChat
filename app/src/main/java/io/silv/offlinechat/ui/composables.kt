package io.silv.offlinechat.ui

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("NewApi")
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    device: WifiP2pDevice,
) {

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(32.dp)),
        elevation = 4.dp,
        shape = RoundedCornerShape(33.dp),
        backgroundColor = MaterialTheme.colors.primaryVariant
    ) {
        Column(Modifier.wrapContentHeight().padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(text = "Device Name:", fontSize = 26.sp)
                Text(text = device.deviceName, color = Color.DarkGray, fontSize = 18.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                Text(text = "Mac Address:", fontSize = 32.sp)
                Text(text = device.deviceAddress, color = Color.DarkGray, fontSize = 20.sp)
            }
            Text(text = "is group owner: ${device.isGroupOwner}")
            Text(text = "wps display supported: ${device.wpsDisplaySupported()}")
            Text(text = "primary device type: ${device.primaryDeviceType}")
            Text(text = "secondary device type: ${device.secondaryDeviceType}")
            Text(text = "device status: ${device.status}")
            Text(text = "device status: ${getWfdInfo(device)}")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun getWfdInfo(device : WifiP2pDevice): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
        "below version code R"
    } else device.wfdInfo.toString()
}