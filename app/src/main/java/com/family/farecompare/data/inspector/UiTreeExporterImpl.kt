package com.family.farecompare.data.inspector

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.family.farecompare.domain.inspector.UiTreeExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Writes to the shared Documents/FareCompare directory. Uses the scoped
 * storage MediaStore API on Android 10+ (no runtime permission required),
 * and falls back to direct file access with the legacy WRITE_EXTERNAL_STORAGE
 * permission on Android 9 and below.
 */
class UiTreeExporterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UiTreeExporter {

    override suspend fun exportToDocuments(fileName: String, content: String): String? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(fileName, content)
                } else {
                    exportViaLegacyFile(fileName, content)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to export UI tree: ${exception.message}")
                null
            }
        }

    private fun exportViaMediaStore(fileName: String, content: String): String? {
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$EXPORT_SUBDIRECTORY"
        val resolver = context.contentResolver
        val uri = findExistingFile(resolver, fileName, relativePath) ?: insertNewFile(resolver, fileName, relativePath)
        uri ?: return null

        resolver.openOutputStream(uri, "wt")?.use { stream -> stream.write(content.toByteArray()) }
            ?: return null

        return "$relativePath/$fileName"
    }

    private fun insertNewFile(resolver: ContentResolver, fileName: String, relativePath: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        return resolver.insert(MediaStore.Files.getContentUri("external"), values)
    }

    private fun findExistingFile(resolver: ContentResolver, fileName: String, relativePath: String): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(fileName, "$relativePath/")

        resolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun exportViaLegacyFile(fileName: String, content: String): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "WRITE_EXTERNAL_STORAGE not granted; cannot export UI tree on this Android version.")
            return null
        }

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            EXPORT_SUBDIRECTORY
        )
        if (!directory.exists() && !directory.mkdirs()) return null

        val file = File(directory, fileName)
        file.writeText(content)
        return file.absolutePath
    }

    private companion object {
        const val TAG = "UiTreeExporter"
        const val EXPORT_SUBDIRECTORY = "FareCompare"
    }
}
