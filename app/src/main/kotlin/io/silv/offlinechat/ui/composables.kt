package io.silv.offlinechat.ui

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

/*
https://cse.iitkgp.ac.in/~bivasm/sp_notes/wifi_direct_2.pdf
2 Primary Device Type
The Primary Device Type attribute contains the primary type of the device. Its
format is defined as follows:
 0 1 2 3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 | Attribute ID | Length |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 | Category ID | OUI (1-2) |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 | OUI (3-4) | Sub Category ID |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
Vendor-specific sub-categories are designated by setting the OUI to the value
associated with that vendor. Note that a four-byte subdivided OUI is used. For
the predefined values, the Wi-Fi Alliance OUI of 00 50 F2 04 is used. The
predefined values for Category ID and Sub Category ID are provided in Table
41 (Primary Device Type) in section 12 (Data Element Definitions) of the Wi-Fi
Simple Configuration specification [2]. There is no way to indicate a vendorspecific main device category. The OUI applies only to the interpretation of the
Sub Category. If a vendor does not use sub categories for their OUI, the threebyte OUI occupies the first three bytes of the OUI field and the fourth byte is set
to zero.
 */
/*
mac address lookup api
https://www.macvendorlookup.com/api
 */

@Composable
fun DeviceConnectionCard(
    modifier: Modifier = Modifier,
    deviceName: String,
    deviceAddress: String,
    deviceType: String,
    onConnectClicked: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(0.9f).height(1.dp).border(2.dp, Color.Black).padding(bottom = 2.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "device",
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 12.dp)
            )
            Text(text = deviceName, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.65f))
            Button(
                onClick = { onConnectClicked() },
                modifier = Modifier.weight(0.35f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Green
                )
            ) {
                Text(text = "connect")
            }

        }
        Box(Modifier.fillMaxWidth(0.9f).height(1.dp).border(1.dp, Color.Black).padding(top = 2.dp   ))
    }
}


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
        backgroundColor = MaterialTheme.colors.background
    ) {
        Column(
            Modifier
                .wrapContentHeight()
                .padding(12.dp)) {
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
            Text(text = "is service discovery capable: ${device.isServiceDiscoveryCapable}")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardPermissionScreen(permissionsState: MultiplePermissionsState) {
    if (permissionsState.allPermissionsGranted) {
        // If all permissions are granted, then show screen with the feature enabled
        Text("Camera and Read storage permissions Granted! Thank you!")
    } else {
        Column {
            Text(
                permissionsState.revokedPermissions.toString()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
        }
    }
}



@RequiresApi(Build.VERSION_CODES.R)
private fun getWfdInfo(device : WifiP2pDevice): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
        "below version code R"
    } else device.wfdInfo.toString()
}