package io.silv.offlinechat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.ui.components.ChatBox
import io.silv.offlinechat.ui.components.ChatListItem


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        val (con, box, spacer) = createRefs()
        SelectionContainer(
            Modifier
                .constrainAs(con) {
                    top.linkTo(parent.top)
                    bottom.linkTo(spacer.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(bottom = 12.dp)
                .imePadding()
        ) {
            val lazyListState = rememberLazyListState()
            LaunchedEffect(key1 = viewModel.messages) {
                lazyListState.animateScrollToItem(0)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                state = lazyListState,
                reverseLayout = true
            ) {
                items(viewModel.messages) {
                    ChatListItem(it = it)
                }
            }
        }
        Spacer(modifier = Modifier
            .constrainAs(spacer) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(box.top)
            }
            .padding(50.dp))
        ChatBox(
            modifier = Modifier
                .constrainAs(box) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                }
                .heightIn(0.dp, 200.dp)
                .background(Color(0xfff4f4f5))
                .padding(12.dp),
            onSend = { message ->
                viewModel.sendMessageUsingKtor(message)
            },
            imageReceiver = viewModel.attachmentReceiver,
        )
    }
}


