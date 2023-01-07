package io.silv.offlinechat.ui

import android.widget.EditText
import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import io.silv.offlinechat.Chat
import io.silv.offlinechat.MainActivityViewModel
import kotlinx.coroutines.launch


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
            ImageEditText(viewModel.imageReceiver) {
                text = it
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Button(onClick = { viewModel.sendMessageUsingKtor(text) }) {
                    Text(text = "send message")
                }
            }
            items(viewModel.messages) {
                when(it) {
                    is Chat.ReceivedImage -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            AndroidView(
                                modifier = Modifier.size(100.dp),
                                factory = { context ->
                                    ImageView(context).apply {
                                        setImageURI(it.uri)
                                    }
                                },
                                update = { iv ->
                                    iv.setImageURI(it.uri)
                                }
                            )
                        }
                    }
                    is Chat.ReceivedMessage-> {
                        Box(modifier =  Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text(it.s)
                        }
                    }
                    is Chat.SentImage -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            AndroidView(
                                modifier = Modifier.size(100.dp),
                                factory = { context ->
                                    ImageView(context).apply {
                                        setImageURI(it.uri)
                                    }
                                },
                                update = { iv ->
                                    iv.setImageURI(it.uri)
                                }
                            )
                        }
                    }
                    is Chat.SentMessage -> {
                        Box(modifier =  Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(it.s)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageEditText(
    receiver: ImageReceiver,
    onTextChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val uriList by receiver.uriFlow.collectAsState(emptyList())

Column(Modifier.fillMaxSize()) {
    LazyRow(
        Modifier
            .fillMaxWidth()
            .height(50.dp)) {
        items(uriList) {uri ->
            AndroidView(
                modifier = Modifier
                    .size(50.dp)
                    .fillMaxWidth(0.8f),
                factory = { context ->
                    ImageView(context).apply {
                        setImageURI(uri)
                        clipToOutline = true
                    }
                },
                update = {
                    it.setImageURI(uri)
                }
            )
        }
        item {
            IconButton(onClick = { scope.launch {
                receiver.backspaceImage()
            } }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "backspace")
            }
            IconButton(onClick = { scope.launch { receiver.clearImages() } }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
            }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .height(50.dp),
        factory = { context ->
            // Creates view
            EditText(context).apply {
                width = maxWidth
                height = 50
                // Sets up listeners for View -> Compose communication

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
