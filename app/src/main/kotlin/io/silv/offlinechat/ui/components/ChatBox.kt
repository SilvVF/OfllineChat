package io.silv.offlinechat.ui.components

import android.widget.EditText
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.ui.setCursorDrawableColor
import kotlinx.coroutines.launch

@Composable
fun ChatBox(
    modifier: Modifier = Modifier,
    imageReceiver: ImageReceiver?,
    onSend: (message: String) -> Unit
) {

    var chatMessage by remember {
        mutableStateOf("")
    }

    var chatError by remember {
        mutableStateOf(false)
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ImageAttachmentList(receiver = imageReceiver)
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray.copy(0.5f))
                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageEditText(
                modifier = Modifier.fillMaxWidth(0.8f),
                receiver = imageReceiver,
                text = chatMessage,
                error = chatError,
                onTextChange = { newMessage ->
                    if (newMessage.isNotBlank()) {
                        chatError = false
                    }
                    chatMessage = newMessage
                }
            )
            IconButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (chatMessage.isBlank()) {
                        chatError = true
                    } else {
                        onSend(chatMessage)
                        chatMessage = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "send message"
                )
            }
        }
    }
}

@Composable
fun ImageEditText(
    modifier: Modifier = Modifier,
    receiver: ImageReceiver?,
    text: String,
    error: Boolean,
    onTextChange: (String) -> Unit
) {
    val textFieldColors = TextFieldDefaults.textFieldColors()
   // val backgroundColor = textFieldColors.backgroundColor(enabled = true).value.toArgb()
    val textColor = textFieldColors.textColor(true).value.toArgb()
    val cursorColor = textFieldColors.cursorColor(isError = false).value.toArgb()

    AndroidView(
            modifier = modifier,
            factory = { context ->
                // Creates view
                EditText(context).apply {
                    hint = "Type Message..."
                    setBackgroundColor(Color.Transparent.toArgb())
                    setTextColor(textColor)
                    setCursorDrawableColor(cursorColor)
                    setText(text)
                    // Sets up listeners for View -> Compose communication
                    ViewCompat.setOnReceiveContentListener(
                        this, ImageReceiver.MIME_TYPES, receiver
                    )
                    addTextChangedListener {
                        onTextChange(it.toString())
                    }
                }
            },
            update = {
                if (text.isEmpty()) {
                    it.setText("")
                }
                it.error = if (error) "type a message to send" else null
            }
        )
}

@Composable
fun ImageAttachmentList(
    modifier: Modifier = Modifier,
    receiver: ImageReceiver?
) {
    val scope = rememberCoroutineScope()

    val uriList by receiver?.uriFlow?.collectAsState(emptyList()) ?: return

    Column(modifier = modifier) {
        LazyRow(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            items(uriList) { uri ->
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
                    IconButton(onClick = {
                        scope.launch {
                            receiver?.backspaceImage()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "backspace"
                        )
                    }
                    IconButton(onClick = { scope.launch { receiver?.clearImages() } }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
                    }
                }
            }
        }
    }
}


@Composable
@Preview(

)
fun ChatBoxPreview() {
    Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
        ChatBox(
            imageReceiver = null,
            onSend = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 200.dp)
                .imePadding(),
        )
    }
}