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
import androidx.core.widget.addTextChangedListener
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.data.AttachmentsRepo
import kotlinx.coroutines.launch


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {

    var text by remember {
        mutableStateOf("")
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
        item {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)) {
                ImageEditText(modifier = Modifier.fillMaxSize()) {
                    text = it
                }
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

    LazyRow {
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
        modifier = modifier,
        factory = { context ->
            // Creates view
            EditText(context).apply {
                width = maxWidth
                // Sets up listeners for View -> Compose communication

                val receiver = ImageReceiver(context, AttachmentsRepo(context))

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
