package com.pelotcl.app.platform

import java.io.File
import java.io.InputStreamReader

actual class FileSystem actual constructor(private val context: PlatformContext) {

    actual fun readAsset(path: String): String {
        return context.assets.open(path).use { stream ->
            InputStreamReader(stream).readText()
        }
    }

    actual fun readAssetBytes(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }

    actual fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).use { true }
        } catch (_: Exception) {
            false
        }
    }

    actual fun filesDir(): String = context.filesDir.absolutePath

    actual fun cacheDir(): String = context.cacheDir.absolutePath

    actual fun readFile(path: String): String? {
        val file = File(context.filesDir, path)
        return if (file.exists()) file.readText() else null
    }

    actual fun writeFile(path: String, content: String) {
        val file = File(context.filesDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    actual fun deleteFile(path: String): Boolean {
        return File(context.filesDir, path).delete()
    }

    actual fun fileExists(path: String): Boolean {
        return File(context.filesDir, path).exists()
    }
}
