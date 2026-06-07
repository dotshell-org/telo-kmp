package com.pelotcl.app.generic.data.config

import android.content.Context

internal object AppConfigValidator {

    private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

    fun validate(context: Context, config: AppConfig): List<String> {
        val errors = mutableListOf<String>()

        validateColors(config, errors)
        validateRegexes(config, errors)
        validateMapStyles(config, errors)
        validateTransport(context, config, errors)

        return errors
    }

    private fun validateColors(config: AppConfig, errors: MutableList<String>) {
        fun check(label: String, value: String) {
            if (!HEX_COLOR.matches(value)) {
                errors += "Invalid hex color for $label: \"$value\""
            }
        }

        check("transport.primaryColor", config.transport.primaryColor)
        check("transport.secondaryColor", config.transport.secondaryColor)
        check("theme.metroLineColor", config.theme.metroLineColor)
        check("theme.tramLineColor", config.theme.tramLineColor)
        check("theme.busLineColor", config.theme.busLineColor)
        check("theme.errorColor", config.theme.errorColor)
        check("theme.successColor", config.theme.successColor)
        check("theme.warningColor", config.theme.warningColor)
        check("theme.disruptionColor", config.theme.disruptionColor)
        check("lineColors.fallback", config.lineColors.fallback)
        config.lineColors.rules.forEachIndexed { index, rule ->
            check("lineColors.rules[$index].color", rule.color)
        }
    }

    private fun validateRegexes(config: AppConfig, errors: MutableList<String>) {
        fun check(label: String, pattern: String) {
            runCatching { Regex(pattern) }.onFailure {
                errors += "Invalid regex for $label: \"$pattern\" (${it.message})"
            }
        }

        config.rules.strongLineRegexes.forEachIndexed { i, r ->
            check("rules.strongLineRegexes[$i]", r)
        }
        config.rules.lineNameRegexes.forEachIndexed { i, r ->
            check("rules.lineNameRegexes[$i]", r)
        }
        config.rules.transportTypes.forEachIndexed { i, t ->
            check("rules.transportTypes[$i].regex", t.regex)
        }
    }

    private fun validateMapStyles(config: AppConfig, errors: MutableList<String>) {
        val keys = config.mapStyles.standard.map { it.key } + config.mapStyles.satellite.key
        if (config.mapStyles.defaultKey !in keys) {
            errors += "mapStyles.defaultKey \"${config.mapStyles.defaultKey}\" not found in standard or satellite styles"
        }
        if (config.mapStyles.standard.isEmpty()) {
            errors += "mapStyles.standard must contain at least one entry"
        }
    }

    private fun validateTransport(context: Context, config: AppConfig, errors: MutableList<String>) {
        if (config.transport.regionBounds.size != 4) {
            errors += "transport.regionBounds must have exactly 4 values (minLat, minLon, maxLat, maxLon), got ${config.transport.regionBounds.size}"
        }

        val holidaysFile = config.transport.schoolHolidaysFile
        val exists = runCatching {
            context.assets.open(holidaysFile).use { true }
        }.getOrDefault(false)
        if (!exists) {
            errors += "transport.schoolHolidaysFile \"$holidaysFile\" not found in assets"
        }
    }
}
