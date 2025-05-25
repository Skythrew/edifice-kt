package io.github.skythrew.edificekt.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class UserInfoWidget(
    val application: String?,
    val i18n: String?,
    val name: String,
    val path: String,
    val mandatory: Boolean,
    val id: String,
    val js: String?
)

@Serializable
data class UserInfo(
    val schoolName: String,
    val classId: String,
    val level: String,
    val email: String,
    val mobile: String,
    val login: String,
    val lastName: String,
    val firstName: String,
    val externalId: String,
    val birthDate: LocalDate,
    val forceChangePassword: Boolean?,
    val needRevalidateTerms: Boolean,
    val deletePending: Boolean,
    val username: String,
    val type: String,
    val hasPw: Boolean,
    val federatedIDP: String,
    val optionEnabled: List<String>,
    val userId: String,
    val uai: List<String>,
    val hasApp: Boolean,
    val ignoreMFA: Boolean,
    val widgets: List<UserInfoWidget>
)
