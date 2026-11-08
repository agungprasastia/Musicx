package com.example.musicx.scrobble

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ListenBrainzException(message: String) : IOException(message)

/** Talks only to api.listenbrainz.org. The user token never leaves the Authorization header. */
class ListenBrainzClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
) {
    /** Returns the user name for a valid token, null for a rejected one. Throws on network failure. */
    suspend fun validate(token: String): String? = withContext(Dispatchers.IO) {
        require(isValidListenBrainzToken(token))
        val request = Request.Builder()
            .url("$BASE_URL/1/validate-token")
            .header("Authorization", "Token $token")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 401) return@use null
            if (!response.isSuccessful) throw ListenBrainzException("HTTP ${response.code}")
            parseValidateResponse(response.peekBody(MAX_BODY_BYTES).string())
        }
    }

    suspend fun submit(token: String, listen: Listen, clientVersion: String) = withContext(Dispatchers.IO) {
        require(isValidListenBrainzToken(token))
        val request = Request.Builder()
            .url("$BASE_URL/1/submit-listens")
            .header("Authorization", "Token $token")
            .post(listenBrainzPayload(listen, clientVersion).toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ListenBrainzException("HTTP ${response.code}")
        }
    }

    private companion object {
        const val BASE_URL = "https://api.listenbrainz.org"
        const val MAX_BODY_BYTES = 64L * 1024
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
