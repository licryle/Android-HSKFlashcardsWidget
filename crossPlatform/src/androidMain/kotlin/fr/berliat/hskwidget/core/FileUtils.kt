package fr.berliat.hskwidget.core

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

import java.util.UUID

object FileUtils {
    fun hasFolderWritePermission(context: Context, uri: Uri): Boolean {
        if (uri.toString() == "") return false
        if (!DocumentsContract.isTreeUri(uri)) return false

        val resolver = context.contentResolver
        val persistedUris = resolver.persistedUriPermissions

        for (permission in persistedUris) {
            if (permission.uri == uri && permission.isWritePermission) {
                return true
            }
        }
        return false
    }

    suspend fun listFilesInSAFDirectory(context: Context, directoryUri: Uri): List<DocumentFile>
            = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, directoryUri)
        dir?.listFiles()?.toList() ?: emptyList()
    }

    suspend fun copyFileUsingSAF(context: Context, sourceFile: File, destinationDir: Uri, fileName: String): Boolean
            = withContext(Dispatchers.IO) {
        try {
            // Open input stream for the source database file
            val inputStream: InputStream = FileInputStream(sourceFile)

            val dir = DocumentFile.fromTreeUri(context, destinationDir)
            val destinationFile = dir?.createFile("application/octet-stream", fileName)

            // Open OutputStream to the destination file
            context.contentResolver.openFileDescriptor(destinationFile!!.uri, "w")
                ?.use { parcelFileDescriptor ->
                    FileOutputStream(parcelFileDescriptor.fileDescriptor).use { output ->
                        // Copy data from source to destination
                        inputStream.use { input ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while (input.read(buffer).also { length = it } > 0) {
                                output.write(buffer, 0, length)
                            }
                        }
                    }
                }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun copyUriToCacheDir(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            val file = File(uri.path!!)
            if (file.absolutePath.startsWith(context.cacheDir.absolutePath)) {
                return@withContext file // already in cacheDir, no need to copy
            }
        }

        val inputStream = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> File(uri.path!!).inputStream()
            else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        } ?: throw IllegalArgumentException("Cannot open input stream from URI")

        val outFile = File(context.cacheDir, UUID.randomUUID().toString())
        outFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        inputStream.close()

        return@withContext outFile
    }

    fun pathToUri(path: Path): Uri {
        val file = File(path.toString())
        return Uri.fromFile(file)
    }
}