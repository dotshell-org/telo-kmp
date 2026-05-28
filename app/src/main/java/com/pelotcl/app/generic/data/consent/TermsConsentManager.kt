package com.pelotcl.app.generic.data.consent

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TermsConsentManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<TermsConsentState> = _state.asStateFlow()

    val isAccepted: Boolean
        get() = _state.value.accepted

    fun accept(version: Int) {
        update(
            TermsConsentState(
                accepted = true,
                decidedAtEpochMs = System.currentTimeMillis(),
                versionAccepted = version
            )
        )
    }

    fun decline() {
        update(
            TermsConsentState(
                accepted = false,
                decidedAtEpochMs = System.currentTimeMillis(),
                versionAccepted = null
            )
        )
    }

    private fun load(): TermsConsentState {
        return TermsConsentState(
            accepted = prefs.getBoolean(KEY_ACCEPTED, false),
            decidedAtEpochMs = prefs.getLong(KEY_DECIDED_AT, 0L).takeIf { it > 0 },
            versionAccepted = prefs.getInt(KEY_VERSION, -1).takeIf { it >= 0 }
        )
    }

    private fun update(new: TermsConsentState) {
        prefs.edit {
            putBoolean(KEY_ACCEPTED, new.accepted)
            putLong(KEY_DECIDED_AT, new.decidedAtEpochMs ?: 0L)
            if (new.versionAccepted != null) {
                putInt(KEY_VERSION, new.versionAccepted)
            } else {
                remove(KEY_VERSION)
            }
        }
        _state.value = new
    }

    companion object {
        private const val PREFS_NAME = "terms_consent"
        private const val KEY_ACCEPTED = "accepted"
        private const val KEY_DECIDED_AT = "decided_at"
        private const val KEY_VERSION = "version_accepted"
    }
}

data class TermsConsentState(
    val accepted: Boolean = false,
    val decidedAtEpochMs: Long? = null,
    val versionAccepted: Int? = null
)
