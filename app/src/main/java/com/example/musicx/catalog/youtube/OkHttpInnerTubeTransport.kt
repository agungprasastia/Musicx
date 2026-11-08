package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HTTPS-only transport for the guest InnerTube web endpoint.
 *
 * Calls are cancelled with the coroutine, and both known Content-Length and
 * streamed bodies are bounded. Errors carry a status/type, never a full URL.
 */
class OkHttpInnerTubeTransport(
    private val httpClient: OkHttpClient = defaultInnerTubeHttpClient(),
) : InnerTubeTransport {
    override suspend fun post(endpoint: String, jsonBody: String): Result<String> {
        val url = try {
            innerTubeEndpointUrl(endpoint)
        } catch (error: IllegalArgumentException) {
            return Result.failure(CatalogError.InvalidResponse)
        }
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("Origin", MUSIC_ORIGIN)
            .header("Referer", "$MUSIC_ORIGIN/")
            .header("X-YouTube-Client-Name", WEB_REMIX_CLIENT_ID)
            .header("X-YouTube-Client-Version", WEB_REMIX_VERSION)
            .header("Accept-Language", "en")
            .build()

        return try {
            val response = execute(request)
            withContext(Dispatchers.IO) { readResponse(response) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: CatalogError) {
            Result.failure(error)
        } catch (error: IOException) {
            Result.failure(CatalogError.Network(error))
        }
    }

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            }
        })
    }

    private fun readResponse(response: Response): Result<String> = response.use {
        if (!it.isSuccessful) return Result.failure(CatalogError.Http(it.code))
        val body = it.body ?: return Result.failure(CatalogError.InvalidResponse)
        if (body.contentLength() > MAX_RESPONSE_BYTES) {
            return Result.failure(CatalogError.ResponseTooLarge)
        }
        return try {
            val text = readBoundedUtf8(body.source(), MAX_RESPONSE_BYTES)
            if (text.isBlank()) Result.failure(CatalogError.InvalidResponse) else Result.success(text)
        } catch (error: CatalogError) {
            Result.failure(error)
        }
    }

    private companion object {
        const val MUSIC_ORIGIN = "https://music.youtube.com"
        const val WEB_REMIX_CLIENT_ID = "67"
        const val WEB_REMIX_VERSION = "1.20250101.01.00"
        const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal val INNERTUBE_ALLOWED_ENDPOINTS = setOf("search", "browse", "player")

internal fun innerTubeEndpointUrl(endpoint: String): HttpUrl {
    require(endpoint in INNERTUBE_ALLOWED_ENDPOINTS) { "Unsupported InnerTube endpoint" }
    val url = "https://music.youtube.com/"
        .toHttpUrl()
        .newBuilder()
        .addPathSegment("youtubei")
        .addPathSegment("v1")
        .addPathSegment(endpoint)
        .addQueryParameter("prettyPrint", "false")
        .addQueryParameter("hl", "en")
        .build()
    check(url.isHttps && url.host == "music.youtube.com") { "InnerTube endpoint must be HTTPS" }
    return url
}

internal fun defaultInnerTubeHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .callTimeout(20, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build()

internal fun readBoundedUtf8(source: BufferedSource, maxBytes: Long): String {
    val buffer = Buffer()
    var total = 0L
    while (true) {
        val read = source.read(buffer, READ_CHUNK_BYTES)
        if (read == -1L) break
        total += read
        if (total > maxBytes) throw CatalogError.ResponseTooLarge
    }
    return buffer.readUtf8()
}

private const val READ_CHUNK_BYTES = 8L * 1024
