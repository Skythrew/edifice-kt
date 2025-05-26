package io.github.skythrew.edificekt.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestError(
    val error: String,
    @SerialName("error_description") val description: String? = null
)