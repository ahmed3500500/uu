package com.snapsave.pro

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

object MediaSaver {

    fun saveVideoToGallery(context: Context, videoFile: File, fileName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(videoFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            } else {
                return false
            }
        } else {
            // For Android 9 and below, we just use the file system directly (already handled by PRDownloader if path is public)
            // But since we are moving from private cache to public, we might need to copy if not already there.
            // Assuming we download to cache dir first now.
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDir.exists()) publicDir.mkdirs()
            val destFile = File(publicDir, fileName)
            try {
                videoFile.copyTo(destFile, overwrite = true)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }

    fun saveAudioToGallery(context: Context, audioFile: File, fileName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC) // Or Downloads
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(audioFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            } else {
                return false
            }
        } else {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!publicDir.exists()) publicDir.mkdirs()
            val destFile = File(publicDir, fileName)
            try {
                audioFile.copyTo(destFile, overwrite = true)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }
}
