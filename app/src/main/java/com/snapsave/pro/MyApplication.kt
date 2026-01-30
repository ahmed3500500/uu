package com.snapsave.pro

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("MyApplication", "failed to initialize youtubedl-android", e)
        }

        // Setup Global Crash Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val logContent = """
                |$timestamp
                |---------------------------------------
                |$stackTrace
                |---------------------------------------
                |
            """.trimMargin()

            // Try to save to External Files Dir (No permission needed)
            // Path: /Android/data/com.snapsave.pro/files/crash_log.txt
            val externalDir = getExternalFilesDir(null)
            if (externalDir != null) {
                val file = File(externalDir, "crash_log.txt")
                file.appendText(logContent)
            }
            
            // Also try to save to Downloads (might fail on Android 11+ without specific setup, but worth a try for easy access)
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val publicFile = File(downloadsDir, "snapsave_crash.txt")
                publicFile.appendText(logContent)
            } catch (e: Exception) {
                // Ignore permissions errors for public dir
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
