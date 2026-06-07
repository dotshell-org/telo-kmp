package com.pelotcl.app.generic.data.models

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedString(
    val value: String?,
    val lang: String?
)
