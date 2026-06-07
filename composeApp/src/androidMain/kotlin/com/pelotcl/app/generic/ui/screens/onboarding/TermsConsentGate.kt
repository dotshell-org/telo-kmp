package com.pelotcl.app.generic.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pelotcl.app.generic.data.config.AppConfigLoader
import com.pelotcl.app.generic.data.consent.TermsConsentManager

@Composable
fun TermsConsentGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val config = AppConfigLoader.getConfig().consent
    val legalSections = AppConfigLoader.getConfig().about.legalSections
    val manager = remember(context) { TermsConsentManager(context) }
    val state by manager.state.collectAsState()

    val needsAcceptance = !state.accepted || (state.versionAccepted ?: 0) < config.version

    if (needsAcceptance) {
        TermsConsentScreen(
            consent = config,
            legalSections = legalSections,
            onAccept = { manager.accept(config.version) }
        )
    } else {
        content()
    }
}
