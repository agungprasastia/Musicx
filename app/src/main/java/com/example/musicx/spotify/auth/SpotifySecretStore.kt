package com.example.musicx.spotify.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SpotifySecretStore {
    fun loadSecret(): SpotifySecret?
    fun saveSecret(secret: SpotifySecret)
    fun clear()
}

class InMemorySpotifySecretStore : SpotifySecretStore {
    private var secret: SpotifySecret? = null

    override fun loadSecret(): SpotifySecret? = secret
    override fun saveSecret(secret: SpotifySecret) { this.secret = secret }
    override fun clear() { secret = null }
}

class EncryptedSpotifySecretStore(context: Context) : SpotifySecretStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun loadSecret(): SpotifySecret? {
        val iv = preferences.getString(IV, null)
        val ciphertext = preferences.getString(CIPHERTEXT, null)
        if (iv == null && ciphertext == null) return null
        if (iv == null || ciphertext == null) {
            clear()
            return null
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            SpotifySecret.parse(String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8))
                ?: clear().let { null }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override fun saveSecret(secret: SpotifySecret) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val values = mapOf(
            IV to Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            CIPHERTEXT to Base64.encodeToString(cipher.doFinal(secret.reveal().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP),
        )
        check(preferences.edit().putString(IV, values.getValue(IV)).putString(CIPHERTEXT, values.getValue(CIPHERTEXT)).commit()) {
            "Could not save Spotify session."
        }
    }

    override fun clear() {
        preferences.edit().clear().commit()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "spotify_session"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
        const val KEY_ALIAS = "musicx.spotify.sp_dc"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
