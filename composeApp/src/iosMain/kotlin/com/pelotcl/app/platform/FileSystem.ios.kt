package com.pelotcl.app.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.create

actual class FileSystem actual constructor(private val context: PlatformContext) {

    actual fun readAsset(path: String): String {
        val components = path.split("/")
        val fileName = components.last()
        val nameComponents = fileName.split(".")
        val name = nameComponents.dropLast(1).joinToString(".")
        val ext = nameComponents.lastOrNull() ?: ""
        val bundlePath = if (components.size > 1) {
            val dir = components.dropLast(1).joinToString("/")
            NSBundle.mainBundle.pathForResource(name, ofType = ext, inDirectory = dir)
        } else {
            NSBundle.mainBundle.pathForResource(name, ofType = ext)
        }
        requireNotNull(bundlePath) { "Asset not found: $path" }
        return NSString.stringWithContentsOfFile(bundlePath, encoding = NSUTF8StringEncoding, error = null)
            ?: error("Failed to read asset: $path")
    }

    actual fun readAssetBytes(path: String): ByteArray {
        val components = path.split("/")
        val fileName = components.last()
        val nameComponents = fileName.split(".")
        val name = nameComponents.dropLast(1).joinToString(".")
        val ext = nameComponents.lastOrNull() ?: ""
        val bundlePath = if (components.size > 1) {
            val dir = components.dropLast(1).joinToString("/")
            NSBundle.mainBundle.pathForResource(name, ofType = ext, inDirectory = dir)
        } else {
            NSBundle.mainBundle.pathForResource(name, ofType = ext)
        }
        requireNotNull(bundlePath) { "Asset not found: $path" }
        val data = NSData.dataWithContentsOfFile(bundlePath)
            ?: error("Failed to read asset bytes: $path")
        return data.bytes?.let { ptr ->
            ByteArray(data.length.toInt()).also { arr ->
                for (i in arr.indices) {
                    @Suppress("UNCHECKED_CAST")
                    arr[i] = (ptr as kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>)[i]
                }
            }
        } ?: ByteArray(0)
    }

    actual fun assetExists(path: String): Boolean {
        val components = path.split("/")
        val fileName = components.last()
        val nameComponents = fileName.split(".")
        val name = nameComponents.dropLast(1).joinToString(".")
        val ext = nameComponents.lastOrNull() ?: ""
        val bundlePath = if (components.size > 1) {
            val dir = components.dropLast(1).joinToString("/")
            NSBundle.mainBundle.pathForResource(name, ofType = ext, inDirectory = dir)
        } else {
            NSBundle.mainBundle.pathForResource(name, ofType = ext)
        }
        return bundlePath != null
    }

    actual fun filesDir(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).first() as String
    }

    actual fun cacheDir(): String {
        return NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory, NSUserDomainMask, true
        ).first() as String
    }

    actual fun readFile(path: String): String? {
        val fullPath = "${filesDir()}/$path"
        return if (NSFileManager.defaultManager.fileExistsAtPath(fullPath)) {
            NSString.stringWithContentsOfFile(fullPath, encoding = NSUTF8StringEncoding, error = null)
        } else null
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    actual fun writeFile(path: String, content: String) {
        val fullPath = "${filesDir()}/$path"
        val dir = fullPath.substringBeforeLast("/")
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir, withIntermediateDirectories = true, attributes = null, error = null
        )
        (content as NSString).writeToFile(fullPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun deleteFile(path: String): Boolean {
        val fullPath = "${filesDir()}/$path"
        return NSFileManager.defaultManager.removeItemAtPath(fullPath, error = null)
    }

    actual fun fileExists(path: String): Boolean {
        val fullPath = "${filesDir()}/$path"
        return NSFileManager.defaultManager.fileExistsAtPath(fullPath)
    }
}
