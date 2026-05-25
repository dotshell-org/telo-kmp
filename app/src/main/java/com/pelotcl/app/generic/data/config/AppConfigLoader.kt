package com.pelotcl.app.generic.data.config

import android.content.Context
import com.google.gson.Gson
import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader

object AppConfigLoader {
    private var config: AppConfig? = null

    fun loadConfig(context: Context): AppConfig {
        if (config != null) return config!!

        return try {
            context.assets.open("config.yml").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val yaml = Yaml()
                @Suppress("UNCHECKED_CAST")
                val rawData = yaml.load<Map<String, Any>>(reader)
                val json = Gson().toJson(rawData)
                config = Gson().fromJson(json, AppConfig::class.java)
                config!!
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load config.yml from assets", e)
        }
    }

    fun getConfig(): AppConfig {
        return config ?: throw IllegalStateException("AppConfig not initialized. Call loadConfig(context) first.")
    }
}
