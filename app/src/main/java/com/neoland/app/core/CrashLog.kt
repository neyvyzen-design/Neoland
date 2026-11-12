package com.neoland.app.core

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catatan error: setiap crash dan kegagalan penting ditulis ke Download/LiquidIsland-log.txt,
 * supaya penyebab force close bisa dibaca. Menulis tidak pernah boleh menjatuhkan aplikasi.
 */
object CrashLog {
    private const val NAME = "LiquidIsland-log.txt"

    @Volatile
    private var app: Context? = null

    fun install(context: Context) {
        if (app != null) return
        app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { write("CRASH [" + t.name + "]", e) }
            previous?.uncaughtException(t, e)
        }
    }

    fun record(tag: String, e: Throwable) {
        runCatching { write("ERROR $tag", e) }
    }

    fun note(text: String) {
        runCatching { write(text, null) }
    }

    @SuppressLint("NewApi")
    private fun write(title: String, e: Throwable?) {
        val ctx = app ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            append("=== ").append(stamp).append(" | ").append(title)
            append(" | v").append(version).append(" | Android ").append(Build.VERSION.RELEASE)
            append(" | ").append(Build.MODEL).append('\n')
            if (e != null) append(Log.getStackTraceString(e)).append('\n')
        }
        val uri = find(ctx) ?: create(ctx) ?: return
        ctx.contentResolver.openOutputStream(uri, "wa")?.use { it.write(text.toByteArray()) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun find(ctx: Context): Uri? =
        ctx.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Downloads._ID),
            MediaStore.Downloads.DISPLAY_NAME + "=?", arrayOf(NAME), null,
        )?.use { c ->
            if (c.moveToFirst()) ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0)) else null
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun create(ctx: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }
        return ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }
}
