package io.silv.offlinechat.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import io.silv.offlinechat.data.ImageFileRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File


class ImageReceiver(
   private val repo: ImageFileRepo,
   private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : OnReceiveContentListener {


    val uriFlow = repo.uriFlow.also { println(it) }

    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val split = payload.partition { item -> item.uri.also { println("URI $it") } != null }
        val uriContent = split.first
        val remaining = split.second
        uriContent?.let {
            receive(view.context, uriContent)
        }
        println(uriContent)
        return remaining
    }


    private fun receive(context: Context, payload: ContentInfoCompat) {
            val applicationContext = context.applicationContext
            val contentResolver = applicationContext.contentResolver
            val uris: List<Uri> = collectUris(payload.clip)
            for (uri in uris) {
                scope.launch {
                    writeToInternal(uri, contentResolver).collect { result ->
                        result.onSuccess {
                            Log.i("uris received", "Processing URI: ${it}")
                        }.onFailure {
                            Log.i("uris failed", "Processing URI: ${it.message}")
                        }
                    }
                }
            }
        }

    private fun writeToInternal(uri: Uri, contentResolver: ContentResolver) = callbackFlow<Result<Uri>> {
        val mimeType = contentResolver.getType(uri)
        Log.i("uris received", "Processing URI: $uri (type: $mimeType)")
        if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
            // Read the image at the given URI and write it to private storage.
            trySend(Result.success(repo.write(uri).second))
        }
        trySend(Result.failure(Exception("Invalid mime type")))
        awaitClose()
    }

    fun backspaceImage() {
        repo.deleteLast()
    }

    fun clearImages() {
        repo.deleteAll()
    }

   fun getAllFiles() : List<File> {
        return repo.allFiles()
    }

    private fun collectUris(clip: ClipData): List<Uri> {
        val uris: MutableList<Uri> = ArrayList(clip.itemCount)
        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri
            if (uri != null) {
                uris.add(uri)
            }
        }
        return uris
    }

    companion object {
        val MIME_TYPES = arrayOf("image/*")
    }

    sealed interface ImageUriAction {
        object Deleted : ImageUriAction
        object Added : ImageUriAction
    }
}

