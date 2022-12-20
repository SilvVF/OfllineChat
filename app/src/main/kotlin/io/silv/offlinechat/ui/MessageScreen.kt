package io.silv.offlinechat.ui

import android.net.Uri
import android.widget.EditText
import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.data.AttachmentsRepo
import kotlinx.coroutines.launch
import java.io.FileNotFoundException


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {

    var text by remember {
        mutableStateOf("")
    }
    Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)) {
            ImageEditText(Modifier) {
                text = it
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Button(onClick = { viewModel.sendMessageFromClient(text) }) {
                    Text(text = "send message")
                }
            }
            items(viewModel.messages) {
                Text(text = it)
            }
        }
    }
}

@Composable
fun ImageEditText(
    modifier: Modifier,
    onTextChange: (String) -> Unit
) {

    val scope = rememberCoroutineScope()

    var uriList by remember {
        mutableStateOf(emptyList<Uri>())
    }
Column(Modifier.fillMaxSize()) {
    LazyRow(
        Modifier
            .fillMaxWidth()
            .height(50.dp)) {
        items(uriList) {uri ->
            AndroidView(
                modifier = Modifier.size(50.dp),
                factory = { context ->
                    ImageView(context).apply {
                        setImageURI(uri)
                        clipToOutline = true
                    }
                }
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize().height(50.dp),
        factory = { context ->
            // Creates view
            EditText(context).apply {
                width = maxWidth
                height = 50
                // Sets up listeners for View -> Compose communication
                val repo = AttachmentsRepo(context)
                val receiver = ImageReceiver(repo)

                scope.launch {
                    receiver.uriFlow.collect {
                        uriList = uriList + it
                    }
                }

                ViewCompat.setOnReceiveContentListener(
                    this, ImageReceiver.MIME_TYPES, receiver
                )
                addTextChangedListener {
                    onTextChange(it.toString())
                }
            }
        },
        update = { }
    )
}
}
