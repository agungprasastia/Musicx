package com.example.musicx.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.musicx.spotify.auth.SpotifySecret
import com.example.musicx.spotify.auth.isSpotifyLoginNavigationAllowed
import com.example.musicx.spotify.auth.parseSpDcCookie

@Composable
fun SpotifyLoginScreen(
    onCookieHarvested: (SpotifySecret) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Text(
            "← Cancel",
            Modifier.clickable(onClick = onCancel).padding(16.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Sign in with Spotify",
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Sign in to import your playlists. Audio is played from YouTube Music.",
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val webView = remember { createLoginWebView(context) }

        DisposableEffect(Unit) {
            CookieManager.getInstance().removeAllCookies(null)
            webView.loadUrl(LOGIN_URL)
            onDispose {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.clearCache(true)
                webView.destroy()
            }
        }

        AndroidView(
            factory = {
                webView.apply {
                    this.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val url = request?.url?.toString()
                            return !isSpotifyLoginNavigationAllowed(url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (url != null && url.startsWith("https://open.spotify.com")) {
                                val cookies = CookieManager.getInstance().getCookie(url) ?: return
                                val secret = parseSpDcCookie(cookies) ?: return
                                onCookieHarvested(secret)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(context: android.content.Context): WebView {
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        @Suppress("DEPRECATION")
        settings.databaseEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
    }
}

private const val LOGIN_URL = "https://accounts.spotify.com/login"
