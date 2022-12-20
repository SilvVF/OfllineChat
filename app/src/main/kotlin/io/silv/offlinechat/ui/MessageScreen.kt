package io.silv.offlinechat.ui

import android.view.View
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import io.silv.offlinechat.MainActivityViewModel


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {

    var text by remember {
        mutableStateOf("")
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                ImageEditText(modifier = Modifier.fillMaxSize()) {
                    text = it
                }
            }
        }
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
private class ImageReceiver: OnReceiveContentListener {

    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val split = payload.partition { item -> item.uri.also { println("URI $it") } != null}
        val uriContent = split.first
        val remaining = split.second
        uriContent?.let {

        }
        return remaining
    }

    companion object {
         val MIME_TYPES = arrayOf("image/*")
    }
}

@Composable
fun ImageEditText(
    modifier: Modifier,
    onTextChange: (String) -> Unit
) {

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Creates view
            EditText(context).apply {
                // Sets up listeners for View -> Compose communication
                ViewCompat.setOnReceiveContentListener(
                    this, ImageReceiver.MIME_TYPES, ImageReceiver()
                )
                addTextChangedListener {
                    onTextChange(it.toString())
                }
            }
        },
        update = { }
    )
}
