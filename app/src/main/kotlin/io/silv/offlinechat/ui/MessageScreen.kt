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
import io.silv.offlinechat.MainActivityViewModel
import kotlinx.coroutines.launch


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {

    var text by remember {
        mutableStateOf("")
    }

    Box(modifier = Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(viewModel.messages) {
                ChatMessage(it = it)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
        ) {
                Button(onClick = { viewModel.sendMessageUsingKtor(text) }) {
                    Text(text = "send message")
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(10.dp, 100.dp)
                ) {
                    ImageEditText(viewModel.imageReceiver) {
                        text = it
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
            .wrapContentHeight()
    ) {
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
            if (uriList.isNotEmpty()) {
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
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(),
        factory = { context ->
            // Creates view
            EditText(context).apply {
                width = maxWidth
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
