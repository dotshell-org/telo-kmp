@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.pelotcl.app.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * iOS implementation backed by the Keychain.
 */
actual class SecureStorage actual constructor(context: PlatformContext, private val name: String) {

    private fun addOrUpdateKeychainItem(key: String, value: String) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query = NSMutableDictionary.dictionary()
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrService, (name as NSString) as CFStringRef)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrAccount, (key as NSString) as CFStringRef)

        val status = SecItemCopyMatching(query as CFDictionaryRef, null)
        if (status == errSecSuccess) {
            val attributesToUpdate = NSMutableDictionary.dictionary()
            CFDictionarySetValue(attributesToUpdate as CFMutableDictionaryRef, kSecValueData, data as CFDataRef)
            SecItemUpdate(query as CFDictionaryRef, attributesToUpdate as CFDictionaryRef)
        } else if (status == errSecItemNotFound) {
            CFDictionarySetValue(query as CFMutableDictionaryRef, kSecValueData, data as CFDataRef)
            SecItemAdd(query as CFDictionaryRef, null)
        }
    }

    private fun getKeychainItem(key: String): String? {
        val query = NSMutableDictionary.dictionary()
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrService, (name as NSString) as CFStringRef)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrAccount, (key as NSString) as CFStringRef)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecMatchLimit, kSecMatchLimitOne)

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status == errSecSuccess) {
                val data = CFBridgingRelease(result.value) as NSData
                NSString.create(data, NSUTF8StringEncoding) as String
            } else {
                null
            }
        }
    }

    private fun deleteKeychainItem(key: String) {
        val query = NSMutableDictionary.dictionary()
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrService, (name as NSString) as CFStringRef)
        CFDictionarySetValue(query as CFMutableDictionaryRef, kSecAttrAccount, (key as NSString) as CFStringRef)
        SecItemDelete(query as CFDictionaryRef)
    }

    actual fun getString(key: String): String? = getKeychainItem(key)

    actual fun putString(key: String, value: String) {
        addOrUpdateKeychainItem(key, value)
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return getKeychainItem(key)?.toLongOrNull() ?: defaultValue
    }

    actual fun putLong(key: String, value: Long) {
        addOrUpdateKeychainItem(key, value.toString())
    }

    actual fun remove(key: String) {
        deleteKeychainItem(key)
    }
}
