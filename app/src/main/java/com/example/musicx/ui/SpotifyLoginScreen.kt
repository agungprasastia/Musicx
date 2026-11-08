package com.example.musicx.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = {
                    webView.apply {
                        this.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (request.isForMainFrame == false) return false
                                return !isSpotifyLoginNavigationAllowed(url)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                errorMessage = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                if (url != null && url.startsWith("https://open.spotify.com")) {
                                    val cookies = CookieManager.getInstance().getCookie(url) ?: return
                                    val secret = parseSpDcCookie(cookies) ?: return
                                    onCookieHarvested(secret)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    errorMessage = "Unable to connect to Spotify login page."
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            webView.loadUrl(LOGIN_URL)
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
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

        // Remove WebView marker so accounts.spotify.com allows modern web auth
        val defaultUa = settings.userAgentString
        settings.userAgentString = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
    }
}

private const val LOGIN_URL = "https://accounts.spotify.com/login"
