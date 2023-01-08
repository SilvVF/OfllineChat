package io.silv.offlinechat.data



import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.google.common.io.ByteStreams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


/**
 * Stores attachments as files in the app's private storage directory (see
 * [Context.getDataDir], [Context.getFilesDir], etc).
 */
class ImageFileRepo(
    private val context: Context,
    private val child: String,
    val fileDeletionLock: Mutex = Mutex(false)
) {

    private val attachmentsDir: File = File(context.filesDir, child)
    private val fileEventChannel: Channel<ImageFileEvent> = Channel()

    sealed interface ImageFileEvent
    data class DeletedAt(val i: Int) : ImageFileEvent
    data class Added(val uri: Uri) : ImageFileEvent

    object DeletedLast: ImageFileEvent
    /**
     * Reads the content at the given URI and writes it to private storage. Then returns a content
     * URI referencing the newly written file. https://developer.android.com/training/data-storage/app-specific
     */

    suspend fun write(uri: Uri): Pair<File, Uri> {
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
                fileEventChannel.trySend(Added(resultUri))
                return newAttachment to resultUri
            }
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }


    suspend fun deleteLast() {
        fileDeletionLock.withLock {
            val lastFile  = attachmentsDir.listFiles()?.lastOrNull()
            if(lastFile?.delete() == true) {
                fileEventChannel.trySend(DeletedLast)
            }
        }
    }
   suspend fun deleteAll() {
       fileDeletionLock.withLock {
           val files = attachmentsDir.listFiles() ?: return
           for ((i, file) in files.withIndex()) {
               if (file.delete()) {
                   fileEventChannel.trySend(DeletedAt(i))
               }
           }
       }
    }

    val uriFlow: StateFlow<List<Uri>> = fileEventChannel.receiveAsFlow().map { event ->
        allUris.first()
    }.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, emptyList())

    private val allUris: Flow<List<Uri>> = flow {
            emit(
                attachmentsDir.listFiles()?.mapNotNull {
                    it.toUri()
                } ?: emptyList<Uri>()
            )
    }.flowOn(Dispatchers.IO)
}