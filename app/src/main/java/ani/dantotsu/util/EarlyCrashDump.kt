package ani.dantotsu.util

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import ani.dantotsu.BuildConfig
import java.io.File

/**
 * Diagnostico de arranque de Kuma: si la app se cierra por una excepcion no controlada,
 * guarda el error en la carpeta Descargas (archivo "Kuma-crash.txt") para poder leerlo
 * desde el telefono sin PC. No envia nada a ningun sitio.
 */
object EarlyCrashDump {
    private var appContext: Context? = null
    private var lastDumped: Throwable? = null

    fun install(context: Context) {
        appContext = context.applicationContext ?: context
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            dump(t, e)
            previous?.uncaughtException(t, e)
        }
    }

    fun dump(thread: Thread, error: Throwable) {
        try {
            if (lastDumped === error) return
            lastDumped = error
            val ctx = appContext ?: return
            val process = if (Build.VERSION.SDK_INT >= 28) Application.getProcessName() else "?"
            val text = buildString {
                append("Kuma ").append(BuildConfig.VERSION_NAME)
                append(" (").append(BuildConfig.APPLICATION_ID).append(")\n")
                append("Android ").append(Build.VERSION.RELEASE)
                append(" (API ").append(Build.VERSION.SDK_INT).append(") - ")
                append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                append("Proceso: ").append(process).append(" | Hilo: ").append(thread.name).append('\n')
                append("Fecha: ").append(java.util.Date()).append("\n\n")
                append(android.util.Log.getStackTraceString(error))
                append("\n")
            }
            write(ctx, text)
        } catch (ignored: Throwable) {
            // nunca romper mas de lo que ya esta roto
        }
    }

    private fun write(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "Kuma-crash.txt")
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        } else {
            File(context.getExternalFilesDir(null), "Kuma-crash.txt").appendText(text)
        }
    }
}
