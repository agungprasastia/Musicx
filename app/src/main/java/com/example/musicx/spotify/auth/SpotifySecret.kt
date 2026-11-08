package com.example.musicx.spotify.auth

/**
 * Bearer-equivalent Spotify `sp_dc` cookie value. The raw value is exposed
 * only to the cookie manager and HTTP header builder through [reveal]; every
 * string representation is redacted so accidental logging cannot leak it.
 */
@JvmInline
value class SpotifySecret private constructor(private val value: String) {
    val length: Int get() = value.length

    internal fun reveal(): String = value

    override fun toString(): String = "SpotifySecret(<redacted>)"

    companion object {
        fun parse(raw: String?): SpotifySecret? {
            if (raw.isNullOrBlank() || raw.length > MAX_LENGTH) return null
            val valid = raw.all { character ->
                character.code in 0x21..0x7E && character != ';'
            }
            return if (valid) SpotifySecret(raw) else null
        }

        private const val MAX_LENGTH = 4_096
    }
}

/**
 * Extracts `sp_dc` from a CookieManager-style header. The first `sp_dc` pair
 * wins; when that pair is present but malformed, parsing fails instead of
 * falling through to a different cookie.
 */
fun parseSpDcCookie(raw: String?): SpotifySecret? {
    if (raw.isNullOrBlank()) return null

    val pairs = raw.split(';').map { it.trim() }.filter { it.isNotEmpty() }
    if (pairs.any { pair -> pair.indexOf('=') <= 0 }) return null

    pairs.forEach { pair ->
        val separator = pair.indexOf('=')
        if (pair.substring(0, separator).trim() != "sp_dc") return@forEach
        return SpotifySecret.parse(pair.substring(separator + 1).trim())
    }
    return null
}
