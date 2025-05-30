package io.github.skythrew.edificekt

import io.github.skythrew.edificekt.managers.ConversationManager
import io.github.skythrew.edificekt.models.UserInfo
import io.github.skythrew.edificekt.resources.Auth
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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.resources.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class EdificeClient (
    val instanceUrl: String,
    private val debug: Boolean = false
) {
    private var _accessToken: String? = null
    private var _refreshToken: String? = null

    val accessToken get() = _accessToken
    val refreshToken get() = _refreshToken

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
                println(exceptionResponse.bodyAsText())
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
    private suspend fun refreshToken(clientId: String, clientSecret: String, refreshToken: String): AuthTokenResponse {
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
     * @param clientId
     * @param clientSecret
     * @param saml The SAMLResponse to use
     * @param scope The tokens' scope (see documentation)
     */
    suspend fun getTokensBySaml(clientId: String, clientSecret: String, saml: String, scope: List<String>): AuthTokenResponse {
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
     * Get authentication tokens by credentials.
     *
     * @param clientId
     * @param clientSecret
     * @param username
     * @param password
     */
    suspend fun getTokensByCredentials(clientId: String, clientSecret: String, username: String, password: String, scope: List<String>): AuthTokenResponse {
        val response = httpClient.submitForm(href(ResourcesFormat(), Auth.Oauth2.Token()), parameters {
            append("client_id", clientId)
            append("client_secret", clientSecret)
            append("grant_type", "password")
            append("username", username)
            append("password", password)
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
        clientId: String,
        clientSecret: String,
        accessTokenParam: String,
        refreshTokenParam: String
    ) {
        httpClient = httpClient.config {
            install(io.ktor.client.plugins.auth.Auth) {
                reAuthorizeOnResponse { response ->
                    response.status.value == 302 // Reauthorize on redirect (which is only done when tokens are expired)
                }

                bearer {
                    loadTokens {
                        _accessToken = accessTokenParam
                        _refreshToken = refreshTokenParam
                        BearerTokens(accessTokenParam, refreshTokenParam)
                    }

                    refreshTokens {
                        val tokenResponse: AuthTokenResponse = refreshToken(clientId, clientSecret, oldTokens?.refreshToken.toString())

                        _accessToken = tokenResponse.accessToken
                        _refreshToken = tokenResponse.refreshToken

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