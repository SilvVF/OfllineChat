package io.silv.offlinechat.data


/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.net.toUri
import com.google.common.collect.ImmutableList
import com.google.common.io.ByteStreams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.atan


/**
 * Stores attachments as files in the app's private storage directory (see
 * [Context.getDataDir], [Context.getFilesDir], etc).
 */
class ImageFileRepo(
    private val context: Context,
    private val child: String = "attachments"
) {

    private val attachmentsDir: File = File(context.filesDir, child)
    private val fileEventChannel: Channel<Int> = Channel()

    /**
     * Reads the content at the given URI and writes it to private storage. Then returns a content
     * URI referencing the newly written file. https://developer.android.com/training/data-storage/app-specific
     */
    fun write(uri: Uri): Pair<File, Uri> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                requireNotNull(inputStream) { uri.toString() }
                attachmentsDir.mkdirs()
                val fileName =
                    "${child.first()}-" + UUID.randomUUID().toString() + "." + ext
                val newAttachment = File(attachmentsDir, fileName)
                FileOutputStream(newAttachment).use { os -> ByteStreams.copy(inputStream, os) }
                val resultUri = newAttachment.toUri()
                Log.i(
                    "Logcat.TAG",
                    "Saved content: originalUri=$uri, resultUri=$resultUri"
                )
                fileEventChannel.trySend(1)
                return newAttachment to resultUri
            }
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    fun allFiles() = attachmentsDir.listFiles()?.mapNotNull { it } ?: emptyList()

    fun deleteLast() {
        val lastFile  = attachmentsDir.listFiles()?.lastOrNull()
        lastFile?.delete()
        fileEventChannel.trySend(1)
    }
    fun deleteAll() {
        val files = attachmentsDir.listFiles() ?: return
        for (file in files) {
            file.delete()
        }
        fileEventChannel.trySend(1)
    }

    val uriFlow: StateFlow<List<Uri>> = fileEventChannel.receiveAsFlow().map { event ->
         allUris.first()
    }.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, emptyList())

    val allUris: Flow<List<Uri>> = flow {
            emit(
                attachmentsDir.listFiles()?.mapNotNull {
                    it.toUri()
                } ?: emptyList<Uri>()
            )
    }.flowOn(Dispatchers.IO)
}