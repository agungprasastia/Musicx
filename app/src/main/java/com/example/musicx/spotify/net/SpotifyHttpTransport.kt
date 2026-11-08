package com.example.musicx.spotify.net

import com.example.musicx.spotify.model.SpotifyError
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SpotifyHttpTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
) {
    suspend fun post(url: String, headers: Map<String, String>, body: String): String = withContext(Dispatchers.IO) {
        val safeUrl = validateSpotifyUrl(url)
        val request = Request.Builder()
            .url(safeUrl)
            .post(body.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 401 || response.code == 403) throw SpotifyError.SessionExpired
                if (!response.isSuccessful) {
                    val errBody = runCatching { response.body?.string() }.getOrNull()
                    android.util.Log.e("SpotifyHttp", "Request failed (${response.code}) url=$safeUrl body=$errBody")
                    throw SpotifyError.Http(response.code)
                }
                val responseBody = response.body ?: throw SpotifyError.InvalidResponse
                if (responseBody.contentLength() > MAX_RESPONSE_BYTES) throw SpotifyError.ResponseTooLarge
                val bytes = responseBody.bytes()
                if (bytes.size > MAX_RESPONSE_BYTES) throw SpotifyError.ResponseTooLarge
                bytes.toString(Charsets.UTF_8)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: SpotifyError) {
            throw error
        } catch (error: IOException) {
            android.util.Log.e("SpotifyHttp", "Network error calling $safeUrl: ${error.message}", error)
            throw SpotifyError.Network(error)
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024

        fun validateSpotifyUrl(raw: String): String {
            val uri = runCatching { java.net.URI(raw) }.getOrNull() ?: throw SpotifyError.InvalidResponse
            val host = uri.host?.lowercase() ?: throw SpotifyError.InvalidResponse
            if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443) || host !in ALLOWED_HOSTS) {
                throw SpotifyError.InvalidResponse
            }
            return uri.toASCIIString()
        }

        private val ALLOWED_HOSTS = setOf(
            "open.spotify.com",
            "accounts.spotify.com",
            "clienttoken.spotify.com",
            "api-partner.spotify.com",
        )
    }
}
