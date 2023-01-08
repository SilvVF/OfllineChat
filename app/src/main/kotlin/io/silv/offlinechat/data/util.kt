package io.silv.offlinechat.data

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.*

fun writeBytesToFileRepo(fileRepo: ImageFileRepo, image: Image) = flow<Uri> {
        val file = File.createTempFile("temp-image", "${UUID.randomUUID()}")
        file.writeBytes(image.bytes)
        val uri = fileRepo.write(file.toUri()).second
        emit(uri)
        coroutineScope { file.delete() }
}.flowOn(Dispatchers.IO)