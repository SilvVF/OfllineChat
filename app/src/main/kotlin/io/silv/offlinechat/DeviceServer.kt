package io.silv.offlinechat

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import kotlin.math.truncate

var globalPortNumber = 4598
data class DeviceServer(
    val info: WifiP2pInfo, // contains ip addr
)
suspend fun DeviceServer.run() = withContext(Dispatchers.IO) {
    val serverSocket = ServerSocket(globalPortNumber, 500, this@run.info.groupOwnerAddress)
    val socket = serverSocket.accept()
    launch {
        writeFromSocket(socket, "hello from the server")
    }
    socket?.let {
        Log.d("peers", """
                ServerSocket -
                + .channel ${it.channel}
                + .inetAddress ${it.inetAddress}
                + .isBound ${it.isBound}
                + .localPort ${it.localPort}
                + .isClosed ${it.isClosed}
                + .localSocketAddress ${it.localSocketAddress}
                + .receiveBufferSize ${it.receiveBufferSize}
                + .reuseAddress ${it.reuseAddress}
                + .soTimeout ${it.soTimeout}
            """.trimIndent()

        )
    }
    val input = BufferedReader(InputStreamReader(socket.getInputStream()));
    while( true) {
        runCatching {
            input.readLine()?.let {
                Log.d("server", "received : $it")
            }
        }
    }
    Unit
}
private suspend fun writeFromSocket(
    socket: Socket,
    message: String
) = withContext(Dispatchers.IO) {
    val output = PrintWriter(socket.getOutputStream());
    output.write(message)
    output.flush()
    Log.d("server", "sent: $message")
}
private suspend fun readLoop(
    socket: Socket
) = withContext(Dispatchers.IO) {
    val input = BufferedReader(InputStreamReader(socket.getInputStream()));
    forever {
        runCatching {
            input.readLine()?.let {
                Log.d("server", "received : $it")
            }
        }
    }
}




data class DeviceClient(
    val info: WifiP2pInfo
)

suspend fun DeviceClient.run() = withContext(Dispatchers.IO) {
        val socket = Socket(
            info.groupOwnerAddress.hostAddress,
            globalPortNumber,
            InetAddress.getLocalHost(),
            globalPortNumber
        )
        socket.let {
            Log.d(
                "client", """
                       Socket -
                        + .channel ${it.channel}
                        + .inetAddress ${it.inetAddress}
                        + .isBound ${it.isBound}
                        + .localPort ${it.localPort}
                        + .isClosed ${it.isClosed}
                        + .localSocketAddress ${it.localSocketAddress}
                        + .receiveBufferSize ${it.receiveBufferSize}
                        + .reuseAddress ${it.reuseAddress}
                        + .soTimeout ${it.soTimeout}
                    """.trimIndent()

            )
        }
        launch {
            readLoop(socket)
        }
        //send data to the server device
        while (true) {
            writeFromSocket(
                socket,
                "hello from client"
            )
            delay(2000)
        }
}

fun forever(executable: () -> Unit) {
    while (true) {
        executable()
    }
}