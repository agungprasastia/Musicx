package com.example.musicx.playback.resolver

import com.example.musicx.catalog.youtube.InnerTubeTransport
import com.example.musicx.core.model.CatalogError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class YouTubeStreamResolverTest {
    private class FakePlayerClient(
        private val response: Result<String> = Result.success(playerJson()),
        private val delayMs: Long = 0,
    ) : YouTubePlayerClient {
        var calls = 0
        var lastTrackId: String? = null

        override suspend fun player(trackId: String): Result<String> {
            calls++
            lastTrackId = trackId
            if (delayMs > 0) delay(delayMs)
            return response
        }
    }

    private class RecordingTransport(
        private val result: Result<String>,
    ) : InnerTubeTransport {
        var endpoint: String? = null
        var body: String? = null

        override suspend fun post(endpoint: String, jsonBody: String): Result<String> {
            this.endpoint = endpoint
            body = jsonBody
            return result
        }
    }

    private fun resolver(
        client: FakePlayerClient,
        timeoutMs: Long = 5_000,
    ) = YouTubeStreamResolver(client, clock = { FIXED_NOW }, requestTimeoutMs = timeoutMs)

    @Test
    fun playerClientBuildsGuestWebRemixPlayerRequest() = runBlocking {
        val transport = RecordingTransport(Result.success(playerJson()))
        val client = InnerTubeYouTubePlayerClient(transport)

        client.player("K8v_DaCcORQ").getOrThrow()

        assertEquals("player", transport.endpoint)
        val body = Json.parseToJsonElement(transport.body!!).jsonObject
        assertEquals("K8v_DaCcORQ", body["videoId"]!!.jsonPrimitive.content)
        val clientContext = body["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("WEB_REMIX", clientContext["clientName"]!!.jsonPrimitive.content)
        assertEquals("true", body["contentCheckOk"]!!.jsonPrimitive.content)
    }

    @Test
    fun playerClientMapsTransportFailureToStreamError() = runBlocking {
        val transport = RecordingTransport(Result.failure(CatalogError.Http(429)))

        val result = InnerTubeYouTubePlayerClient(transport).player("K8v_DaCcORQ")

        val error = result.exceptionOrNull()
        assertTrue(error is StreamResolveError.Http)
        assertEquals(429, (error as StreamResolveError.Http).statusCode)
    }

    @Test
    fun blankTrackIdFailsBeforeClientCall() = runBlocking {
        val client = FakePlayerClient()

        val result = resolver(client).resolve("   ")

        assertTrue(result.exceptionOrNull() is StreamResolveError.EmptyTrackId)
        assertEquals(0, client.calls)
    }

    @Test
    fun selectsHighestBitrateDirectAudioStream() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    formats = """
                        ${format(itag = 140, mime = "audio/mp4", bitrate = 128_000, url = audioUrl(140))},
                        ${format(itag = 251, mime = "audio/webm", bitrate = 160_000, url = audioUrl(251))},
                        ${format(itag = 137, mime = "video/mp4", bitrate = 4_000_000, url = videoUrl(137))}
                    """.trimIndent()
                )
            )
        )

        val stream = resolver(client).resolve("K8v_DaCcORQ").getOrThrow()

        assertEquals("https", stream.uri.scheme)
        assertEquals("rr1---abc.googlevideo.com", stream.uri.host)
        assertTrue(stream.uri.query.contains("itag=251"))
        assertEquals("audio/webm", stream.mimeType)
        assertEquals(FIXED_NOW + 3_600_000L, stream.expiresAtEpochMillis)
    }

    @Test
    fun rejectsHttpAndNonAllowlistedMediaHosts() = runBlocking {
        val http = FakePlayerClient(
            Result.success(
                playerJson(
                    formats = format(
                        itag = 251,
                        mime = "audio/webm",
                        bitrate = 160_000,
                        url = "http://rr1---abc.googlevideo.com/videoplayback?itag=251",
                    )
                )
            )
        )
        assertTrue(resolver(http).resolve("K8v_DaCcORQ").exceptionOrNull() is StreamResolveError.UnsupportedStream)

        val foreignHost = FakePlayerClient(
            Result.success(
                playerJson(
                    formats = format(
                        itag = 251,
                        mime = "audio/webm",
                        bitrate = 160_000,
                        url = "https://evil.example/audio?itag=251",
                    )
                )
            )
        )
        assertTrue(
            resolver(foreignHost).resolve("K8v_DaCcORQ").exceptionOrNull()
                is StreamResolveError.UnsupportedStream
        )
    }

    @Test
    fun cipherProtectedStreamIsRejectedWithoutBypassAttempt() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    formats = """
                        {
                          "itag": 251,
                          "mimeType": "audio/webm",
                          "bitrate": 160000,
                          "signatureCipher": "s=secret&sp=sig&url=https%3A%2F%2Frr1---abc.googlevideo.com%2Fvideoplayback"
                        }
                    """.trimIndent()
                )
            )
        )

        val error = resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull()

        assertTrue(error is StreamResolveError.CipherProtected)
        assertFalse(error!!.message.orEmpty().contains("googlevideo.com"))
    }

    @Test
    fun expiredResponseIsRejected() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    expiresInSeconds = 0,
                    formats = format(itag = 251, mime = "audio/webm", bitrate = 160_000, url = audioUrl(251)),
                )
            )
        )

        assertTrue(resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull() is StreamResolveError.Expired)
    }

    @Test
    fun expiredUrlParameterIsRejectedEvenWithoutExpiresInSeconds() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    expiresInSeconds = null,
                    formats = format(
                        itag = 251,
                        mime = "audio/webm",
                        bitrate = 160_000,
                        url = "https://rr1---abc.googlevideo.com/videoplayback?itag=251&expire=1",
                    ),
                )
            )
        )

        assertTrue(resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull() is StreamResolveError.Expired)
    }

    @Test
    fun loginRequiredIsTypedAndDoesNotExposeReasonDetails() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    status = "LOGIN_REQUIRED",
                    reason = "Please sign in https://rr1---abc.googlevideo.com/videoplayback?token=secret",
                    formats = "",
                )
            )
        )

        val error = resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull()

        assertTrue(error is StreamResolveError.LoginRequired)
        assertFalse(error!!.message.orEmpty().contains("googlevideo.com"))
        assertFalse(error.toString().contains("secret"))
    }

    @Test
    fun unplayableStatusIsTyped() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(status = "UNPLAYABLE", reason = "Video unavailable", formats = "")
            )
        )

        assertTrue(resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull() is StreamResolveError.Unavailable)
    }

    @Test
    fun malformedJsonAndNonObjectRootAreTypedInvalid() = runBlocking {
        val malformed = FakePlayerClient(Result.success("{not json"))
        assertTrue(
            resolver(malformed).resolve("K8v_DaCcORQ").exceptionOrNull()
                is StreamResolveError.InvalidResponse
        )

        val arrayRoot = FakePlayerClient(Result.success("[]"))
        assertTrue(
            resolver(arrayRoot).resolve("K8v_DaCcORQ").exceptionOrNull()
                is StreamResolveError.InvalidResponse
        )
    }

    @Test
    fun noAudioFormatsIsTyped() = runBlocking {
        val client = FakePlayerClient(
            Result.success(
                playerJson(
                    formats = format(itag = 137, mime = "video/mp4", bitrate = 4_000_000, url = videoUrl(137))
                )
            )
        )

        assertTrue(resolver(client).resolve("K8v_DaCcORQ").exceptionOrNull() is StreamResolveError.NoAudioStream)
    }

    @Test
    fun upstreamFailureIsPreserved() = runBlocking {
        val failure = StreamResolveError.Http(403)
        val client = FakePlayerClient(Result.failure(failure))

        val result = resolver(client).resolve("K8v_DaCcORQ")

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun timeoutIsTyped() = runBlocking {
        val client = FakePlayerClient(delayMs = 10_000)

        val result = resolver(client, timeoutMs = 50).resolve("K8v_DaCcORQ")

        assertTrue(result.exceptionOrNull() is StreamResolveError.Timeout)
    }

    @Test
    fun cancellationPropagates() {
        val client = object : YouTubePlayerClient {
            override suspend fun player(trackId: String): Result<String> =
                throw CancellationException("cancelled")
        }

        try {
            runBlocking { YouTubeStreamResolver(client).resolve("K8v_DaCcORQ") }
            fail("Expected cancellation to propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun networkFailureDoesNotExposeFullUrl() {
        val error = StreamResolveError.Network(
            IOException("https://rr1---abc.googlevideo.com/videoplayback?expire=1&sig=secret")
        )

        assertFalse(error.message.orEmpty().contains("googlevideo.com"))
        assertFalse(error.toString().contains("googlevideo.com"))
        assertFalse(error.toString().contains("secret"))
    }

    private companion object {
        const val FIXED_NOW = 1_700_000_000_000L
        const val WEB_HOST = "rr1---abc.googlevideo.com"

        fun audioUrl(itag: Int) =
            "https://$WEB_HOST/videoplayback?expire=9999999999&itag=$itag&mime=audio"

        fun videoUrl(itag: Int) =
            "https://$WEB_HOST/videoplayback?expire=9999999999&itag=$itag&mime=video"

        fun format(itag: Int, mime: String, bitrate: Int, url: String): String = """
            {
              "itag": $itag,
              "mimeType": "$mime",
              "bitrate": $bitrate,
              "url": "$url"
            }
        """.trimIndent()

        fun playerJson(
            status: String = "OK",
            reason: String? = null,
            formats: String = format(251, "audio/webm", 160_000, audioUrl(251)),
            expiresInSeconds: Int? = 3_600,
        ): String {
            val reasonJson = reason?.let { """, "reason": "$it"""" } ?: ""
            val expiresJson = expiresInSeconds?.let { """, "expiresInSeconds": $it""" } ?: ""
            return """
                {
                  "playabilityStatus": {"status": "$status"$reasonJson},
                  "streamingData": {
                    "adaptiveFormats": [$formats]
                    $expiresJson
                  }
                }
            """.trimIndent()
        }
    }
}
