package com.example.musicx.spotify.auth

import com.example.musicx.spotify.model.SpotifyError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

data class SpotifyWebToken(
    val accessToken: String,
    val clientId: String?,
    val expiresAtEpochMillis: Long,
    val isAnonymous: Boolean,
)

data class SpotifyClientToken(
    val token: String,
    val expiresInSeconds: Long,
)

data class SpotifyTokens(
    val bearer: String,
    val clientToken: String,
    val expiresAtEpochMillis: Long,
)

private val tokenJson = Json { ignoreUnknownKeys = true }

fun parseWebToken(json: String, now: Long): Result<SpotifyWebToken> {
    val root = parseObject(json) ?: return Result.failure(SpotifyError.InvalidResponse)
    val token = root.string("accessToken")
    if (!isValidToken(token)) return Result.failure(SpotifyError.InvalidResponse)

    val anonymous = root.boolean("isAnonymous") ?: false
    if (anonymous) return Result.failure(SpotifyError.LoginRejected)

    val expiresAt = root.long("accessTokenExpirationTimestampMs")
        ?: (now + DEFAULT_TOKEN_LIFETIME_MS)
    if (expiresAt <= now) return Result.failure(SpotifyError.SessionExpired)

    return Result.success(
        SpotifyWebToken(
            accessToken = token!!,
            clientId = root.string("clientId")?.takeIf { it.isNotBlank() },
            expiresAtEpochMillis = expiresAt,
            isAnonymous = false,
        )
    )
}

fun parseClientToken(json: String): Result<SpotifyClientToken> {
    val root = parseObject(json) ?: return Result.failure(SpotifyError.InvalidResponse)
    if (root.string("response_type") != "RESPONSE_GRANTED_TOKEN_RESPONSE") {
        return Result.failure(SpotifyError.InvalidResponse)
    }
    val granted = root["granted_token"] as? JsonObject
        ?: return Result.failure(SpotifyError.InvalidResponse)
    val token = granted.string("token")
    if (!isValidToken(token)) return Result.failure(SpotifyError.InvalidResponse)
    val expiresInSeconds = granted.long("expires_after_seconds")
        ?.takeIf { it > 0 }
        ?: DEFAULT_CLIENT_TOKEN_LIFETIME_SECONDS

    return Result.success(SpotifyClientToken(token!!, expiresInSeconds))
}

private fun parseObject(raw: String): JsonObject? = try {
    tokenJson.parseToJsonElement(raw) as? JsonObject
} catch (error: Exception) {
    null
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull

private fun JsonObject.boolean(key: String): Boolean? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
}

private fun isValidToken(value: String?): Boolean =
    !value.isNullOrBlank() &&
        value.length <= MAX_TOKEN_LENGTH &&
        value.all { it.code > 0x20 && !it.isISOControl() }

private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L
private const val DEFAULT_CLIENT_TOKEN_LIFETIME_SECONDS = 3_600L
private const val MAX_TOKEN_LENGTH = 8_192
