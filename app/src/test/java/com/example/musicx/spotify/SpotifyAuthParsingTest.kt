package com.example.musicx.spotify

import com.example.musicx.spotify.auth.parseClientToken
import com.example.musicx.spotify.auth.parseSpDcCookie
import com.example.musicx.spotify.auth.parseWebToken
import com.example.musicx.spotify.model.SpotifyError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyAuthParsingTest {
    @Test
    fun parsesFirstValidSpDcCookiePair() {
        val secret = parseSpDcCookie("a=1; sp_dc=AQabc-123_xyz; sp_dc=second")

        assertEquals("AQabc-123_xyz", secret?.reveal())
    }

    @Test
    fun rejectsBlankMissingAndMalformedCookies() {
        assertNull(parseSpDcCookie(null))
        assertNull(parseSpDcCookie(""))
        assertNull(parseSpDcCookie("a=1; b=2"))
        assertNull(parseSpDcCookie("sp_dc="))
        assertNull(parseSpDcCookie("sp_dc=has space"))
        assertNull(parseSpDcCookie("sp_dc=has;semicolon"))
        assertNull(parseSpDcCookie("sp_dc=line\nbreak"))
    }

    @Test
    fun secretNeverRendersItsValue() {
        val secret = parseSpDcCookie("sp_dc=super-secret-cookie")!!

        assertFalse(secret.toString().contains("super-secret-cookie"))
        assertTrue(secret.toString().contains("redacted"))
    }

    @Test
    fun parsesLoggedInWebToken() {
        val token = parseWebToken(
            json = """
                {
                  "accessToken": "BQ-web-token",
                  "clientId": "client-1",
                  "isAnonymous": false,
                  "accessTokenExpirationTimestampMs": 5000000
                }
            """.trimIndent(),
            now = 1_000_000,
        ).getOrThrow()

        assertEquals("BQ-web-token", token.accessToken)
        assertEquals("client-1", token.clientId)
        assertEquals(5_000_000L, token.expiresAtEpochMillis)
        assertFalse(token.isAnonymous)
    }

    @Test
    fun rejectsAnonymousToken() {
        val result = parseWebToken(
            json = """{"accessToken":"anon","isAnonymous":true,"accessTokenExpirationTimestampMs":5000}""",
            now = 1_000,
        )

        assertTrue(result.exceptionOrNull() is SpotifyError.LoginRejected)
    }

    @Test
    fun rejectsExpiredToken() {
        val result = parseWebToken(
            json = """{"accessToken":"expired","isAnonymous":false,"accessTokenExpirationTimestampMs":999}""",
            now = 1_000,
        )

        assertTrue(result.exceptionOrNull() is SpotifyError.SessionExpired)
    }

    @Test
    fun rejectsMissingOrMalformedToken() {
        assertTrue(
            parseWebToken("""{"isAnonymous":false}""", now = 1_000)
                .exceptionOrNull() is SpotifyError.InvalidResponse
        )
        assertTrue(
            parseWebToken("""{"accessToken":"has space","accessTokenExpirationTimestampMs":5000}""", now = 1_000)
                .exceptionOrNull() is SpotifyError.InvalidResponse
        )
        assertTrue(parseWebToken("{not json", now = 1_000).exceptionOrNull() is SpotifyError.InvalidResponse)
    }

    @Test
    fun parsesGrantedClientToken() {
        val token = parseClientToken(
            """
                {
                  "response_type": "RESPONSE_GRANTED_TOKEN_RESPONSE",
                  "granted_token": {"token": "client-token", "expires_after_seconds": 1800}
                }
            """.trimIndent()
        ).getOrThrow()

        assertEquals("client-token", token.token)
        assertEquals(1_800L, token.expiresInSeconds)
    }

    @Test
    fun rejectsUngrantedClientToken() {
        assertTrue(
            parseClientToken("""{"response_type":"RESPONSE_DENIED","granted_token":{"token":"x"}}""")
                .exceptionOrNull() is SpotifyError.InvalidResponse
        )
        assertTrue(
            parseClientToken("""{"response_type":"RESPONSE_GRANTED_TOKEN_RESPONSE"}""")
                .exceptionOrNull() is SpotifyError.InvalidResponse
        )
    }
}
