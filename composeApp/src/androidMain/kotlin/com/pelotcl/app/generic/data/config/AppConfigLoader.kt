package com.pelotcl.app.generic.data.config

import android.content.Context
import android.util.Log
import com.pelotcl.app.generic.data.GsonProvider
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.InputStreamReader

object AppConfigLoader {
    private const val TAG = "AppConfigLoader"
    private const val CONFIG_FILE = "config.yml"

    @Volatile
    private var config: AppConfig? = null

    private val yaml: Yaml by lazy {
        Yaml(SafeConstructor(LoaderOptions()))
    }

    fun loadConfig(context: Context): AppConfig {
        config?.let { return it }
        synchronized(this) {
            config?.let { return it }
            val loaded = parse(context)
            config = loaded
            return loaded
        }
    }

    fun getConfig(): AppConfig {
        return config ?: throw IllegalStateException(
            "AppConfig not initialized. Call loadConfig(context) first."
        )
    }

    private fun parse(context: Context): AppConfig {
        return try {
            context.assets.open(CONFIG_FILE).use { inputStream ->
                val rawData = InputStreamReader(inputStream).use { reader ->
                    yaml.load<Any?>(reader)
                }
                val gson = GsonProvider.instance
                val tree = gson.toJsonTree(rawData)
                val parsed = gson.fromJson(tree, AppConfig::class.java)
                    ?: throw ConfigLoadException("config.yml deserialized to null")
                val errors = AppConfigValidator.validate(context, parsed)
                if (errors.isNotEmpty()) {
                    val message = buildString {
                        appendLine("config.yml validation failed:")
                        errors.forEach { appendLine("  - $it") }
                    }
                    throw ConfigLoadException(message)
                }
                parsed
            }
        } catch (e: ConfigLoadException) {
            Log.e(TAG, e.message ?: "Config validation error", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $CONFIG_FILE from assets", e)
            throw ConfigLoadException(
                "Failed to load $CONFIG_FILE: ${e.message ?: e.javaClass.simpleName}",
                e
            )
        }
    }
}

class ConfigLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
