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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch



class ImageReceiver(
   private val repo: ImageFileRepo,
   private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : OnReceiveContentListener {


    val uriFlow = repo.uriFlow.also { println(it) }

    fun lockRepoForOp(executable: (locked: Boolean) -> Unit){
        executable(repo.fileDeletionLock.tryLock(this))
        repo.fileDeletionLock.unlock(this)
    }

    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val split = payload.partition { item -> item.uri.also { println("URI $it") } != null }
        val uriContent = split.first
        val remaining = split.second
        uriContent?.let {
            scope.launch {
                receive(view.context, uriContent)
            }
        }
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

    private fun writeToInternal(uri: Uri, contentResolver: ContentResolver) = flow {
        val mimeType = contentResolver.getType(uri)
        Log.i("uris received", "Processing URI: $uri (type: $mimeType)")
        if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
            // Read the image at the given URI and write it to private storage.
            emit(Result.success(repo.write(uri).second))
        } else {
            emit(Result.failure(Exception("Invalid mime type")))
        }
    }

    suspend fun backspaceImage() {
        repo.deleteLast()
    }

    suspend fun clearImages() {
        repo.deleteAll()
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
}

