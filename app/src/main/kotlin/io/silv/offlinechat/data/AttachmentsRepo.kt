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
import com.google.common.collect.ImmutableList
import com.google.common.io.ByteStreams
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


/**
 * Stores attachments as files in the app's private storage directory (see
 * [Context.getDataDir], [Context.getFilesDir], etc).
 */
class ImageFileRepo(private val mContext: Context, child: String = "attachments") {

    private val mAttachmentsDir: File = File(mContext.filesDir, child)

    /**
     * Reads the content at the given URI and writes it to private storage. Then returns a content
     * URI referencing the newly written file.
     */
    fun write(uri: Uri): Uri {
        val contentResolver = mContext.contentResolver
        val mimeType = contentResolver.getType(uri)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        try {
            contentResolver.openInputStream(uri).use { `is` ->
                requireNotNull(`is`) { uri.toString() }
                mAttachmentsDir.mkdirs()
                val fileName =
                    "a-" + UUID.randomUUID().toString() + "." + ext
                val newAttachment = File(mAttachmentsDir, fileName)
                FileOutputStream(newAttachment).use { os -> ByteStreams.copy(`is`, os) }
                val resultUri = getUriForFile(newAttachment)
                Log.i(
                    "Logcat.TAG",
                    "Saved content: originalUri=$uri, resultUri=$resultUri"
                )
                return resultUri
            }
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    fun allFiles() = mAttachmentsDir.listFiles()?.mapNotNull { it } ?: emptyList()

    fun deleteLast() {
        val lastFile  = mAttachmentsDir.listFiles()?.lastOrNull()
        lastFile?.delete()
    }
    fun deleteAll() {
        val files = mAttachmentsDir.listFiles() ?: return
        for (file in files) {
            file.delete()
        }
    }

    val allUris: ImmutableList<Uri>
        get() {
            val files = mAttachmentsDir.listFiles()
            if (files == null || files.isEmpty()) {
                return ImmutableList.of()
            }
            val uris: ImmutableList.Builder<Uri> =
                ImmutableList.builderWithExpectedSize(
                    files.size
                )
            for (file in files) {
                uris.add(getUriForFile(file))
            }
            return uris.build()
        }

    private fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, file)
    }

    companion object {
        // This matches the name declared in AndroidManifest.xml
        private const val FILE_PROVIDER_AUTHORITY =
            "androidx.appcompat.demo.receivecontent.fileprovider"
    }
}