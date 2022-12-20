package io.silv.offlinechat.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import io.silv.offlinechat.data.ImageFileRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ImageReceiver(
   val repo: ImageFileRepo
) : OnReceiveContentListener {


    private val _mutableUriFlow = MutableStateFlow<List<Uri>>(emptyList<Uri>())
    val uriFlow = _mutableUriFlow.asStateFlow()

    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val split = payload.partition { item -> item.uri.also { println("URI $it") } != null}
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
        CoroutineScope(Dispatchers.IO).launch {
            val uris: List<Uri> = collectUris(payload.clip)
            for (uri in uris) {
                val mimeType = contentResolver.getType(uri)
                Log.i("uris received", "Processing URI: $uri (type: $mimeType)")
                if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
                    // Read the image at the given URI and write it to private storage.
                    repo.write(uri)
                }
            }
            _mutableUriFlow.emit(repo.allUris)
        }
    }

    fun backspaceImage() {
        CoroutineScope(Dispatchers.IO).launch {
            _mutableUriFlow.emit(repo.allUris.dropLast(1))
            repo.deleteLast()
        }
    }

    fun clearImages() {
        CoroutineScope(Dispatchers.IO).launch {
            _mutableUriFlow.emit(emptyList())
            repo.deleteAll()
        }
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

