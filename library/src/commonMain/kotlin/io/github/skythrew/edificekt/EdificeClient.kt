package io.github.skythrew.edificekt

import io.github.skythrew.edificekt.managers.ConversationManager
import io.github.skythrew.edificekt.models.UserInfo
import io.github.skythrew.edificekt.models.conversation.ConversationFolder
import io.github.skythrew.edificekt.models.conversation.Message
import io.github.skythrew.edificekt.models.conversation.MessageAttachment
import io.github.skythrew.edificekt.resources.Auth
import io.github.skythrew.edificekt.resources.Conversation
import io.github.skythrew.edificekt.responses.AuthTokenResponse
import io.github.skythrew.edificekt.responses.RequestError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.forms.*
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.content
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.resources.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class EdificeClient (
    val clientId: String,
    val clientSecret: String,
    val instanceUrl: String,
    private val debug: Boolean = false
) {
    internal var httpClient = HttpClient {
        defaultRequest {
            url(instanceUrl)
        }
        BrowserUserAgent()


        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(Resources)


        if (debug)
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }

        expectSuccess = true
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                val clientException = exception as? ClientRequestException ?: return@handleResponseExceptionWithRequest
                val exceptionResponse = clientException.response
                val error: RequestError = exceptionResponse.body()

                throw Exception("Edifice Error (${error.error}): ${error.description}")
            }
        }
    }

    private var _userInfo: UserInfo? = null
    val userInfo: UserInfo? get() = _userInfo

    val conversations = ConversationManager(this)

    /**
     * Refresh bearer tokens.
     *
     * @param refreshToken The current refresh token to use to fetch new ones
     *
     */
    suspend fun refreshToken(refreshToken: String): AuthTokenResponse {
        return httpClient.submitForm(href(ResourcesFormat(), Auth.Oauth2.Token()), parameters {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        }).body()
    }

    /**
     * Get authentication tokens by SAML grant type.
     *
     * @param saml The SAMLResponse to use
     * @param scope The tokens' scope (see documentation)
     */
    suspend fun getTokensBySaml(saml: String, scope: List<String>): AuthTokenResponse {
        val response = httpClient.submitForm(href(ResourcesFormat(), Auth.Oauth2.Token()), parameters {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("grant_type", "saml2")
            append("assertion", saml)
            append("scope", scope.joinToString(" "))
        })

        return response.body()
    }

    /**
     * Log the client in thanks to authentication tokens.
     *
     * @param accessToken OAuth2 access token
     * @param refreshToken OAuth2 refresh token
     */
    suspend fun loginByOauth2Token(
        accessToken: String,
        refreshToken: String
    ) {
        httpClient = httpClient.config {
            install(io.ktor.client.plugins.auth.Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(accessToken, refreshToken)
                    }

                    refreshTokens {
                        val tokenResponse: AuthTokenResponse = refreshToken(oldTokens?.refreshToken.toString())

                        BearerTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    }
                }
            }
        }
        _userInfo = getOauth2UserInfo()
    }

    /**
     * Get client user information.
     */
    suspend fun getOauth2UserInfo(): UserInfo = httpClient.get(Auth.Oauth2.UserInfo()).body()
}