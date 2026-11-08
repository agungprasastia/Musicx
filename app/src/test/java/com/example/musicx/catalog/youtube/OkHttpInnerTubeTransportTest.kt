package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OkHttpInnerTubeTransportTest {
    @Test
    fun endpointUrlIsHttpsAndAllowlisted() {
        val url = innerTubeEndpointUrl("search")

        assertEquals("https", url.scheme)
        assertEquals("music.youtube.com", url.host)
        assertEquals("/youtubei/v1/search", url.encodedPath)
        assertEquals("false", url.queryParameter("prettyPrint"))
    }

    @Test
    fun playerEndpointIsAllowlisted() {
        val url = innerTubeEndpointUrl("player")

        assertEquals("https", url.scheme)
        assertEquals("music.youtube.com", url.host)
        assertEquals("/youtubei/v1/player", url.encodedPath)
    }

    @Test
    fun unknownOrTraversalEndpointIsRejected() {
        listOf("search/../../evil", "https://evil.example/search", "", "SEARCH").forEach { endpoint ->
            try {
                innerTubeEndpointUrl(endpoint)
                fail("Expected endpoint rejection for $endpoint")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().isNotBlank())
            }
        }
    }

    @Test
    fun defaultClientHasExplicitTimeoutsAndNoRedirects() {
        val client = defaultInnerTubeHttpClient()

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertEquals(20_000, client.callTimeoutMillis)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
    }

    @Test
    fun boundedReaderAcceptsBodyAtLimit() {
        val body = readBoundedUtf8(Buffer().writeUtf8("abc"), maxBytes = 3)

        assertEquals("abc", body)
    }

    @Test
    fun boundedReaderRejectsOversizedBody() {
        try {
            readBoundedUtf8(Buffer().writeUtf8("abcd"), maxBytes = 3)
            fail("Expected oversized response rejection")
        } catch (expected: CatalogError) {
            assertTrue(expected is CatalogError.ResponseTooLarge)
        }
    }
}
