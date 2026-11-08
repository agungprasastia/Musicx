package com.example.musicx.spotify.auth

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Offscreen WebView that loads `open.spotify.com` with the stored `sp_dc`
 * cookie, waits for the page's own `/api/token` fetch to complete, and
 * captures the response body through a narrow native bridge.
 *
 * Security invariants:
 * - The cookie is injected through [CookieManager], never passed to JS.
 * - The JS bridge exposes only a single `onTokenBody(String)` sink.
 * - The WebView is destroyed and its storage cleared in `finally`.
 */
class SpotifyWebTokenHarvester(private val context: Context) {

    suspend fun harvest(secret: SpotifySecret): SpotifyWebToken =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String>()
            var webView: WebView? = null
            try {
                webView = createHarvestWebView(context, deferred)
                injectCookie(secret)
                webView.loadUrl(PLAYER_URL)
                val body = withTimeoutOrNull(HARVEST_TIMEOUT_MS) { deferred.await() }
                    ?: throw com.example.musicx.spotify.model.SpotifyError.Timeout
                val now = System.currentTimeMillis()
                parseWebToken(body, now).getOrThrow()
            } finally {
                webView?.let { destroyWebView(it) }
            }
        }

    companion object {
        private const val PLAYER_URL = "https://open.spotify.com"
        private const val HARVEST_TIMEOUT_MS = 20_000L
        private const val TOKEN_PATH = "/get_access_token"

        val LOGIN_ALLOWED_HOSTS = setOf(
            "accounts.spotify.com",
            "open.spotify.com",
            "www.spotify.com",
            "login.spotify.com",
            "challenge.spotify.com",
            "accounts.google.com",
            "appleid.apple.com",
            "www.facebook.com",
            "m.facebook.com",
        )
    }

    private fun injectCookie(secret: SpotifySecret) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie("https://open.spotify.com", "sp_dc=${secret.reveal()}; Secure; HttpOnly")
        cookieManager.flush()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createHarvestWebView(ctx: Context, deferred: CompletableDeferred<String>): WebView {
        val wv = WebView(ctx.applicationContext)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.databaseEnabled = false
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false

        wv.addJavascriptInterface(TokenBridge(deferred), "MusicxTokenBridge")

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // ponytail: inject a fetch interceptor to capture /get_access_token response
                view?.evaluateJavascript(
                    """
                    (function() {
                        var orig = window.fetch;
                        window.fetch = function() {
                            var url = arguments[0];
                            return orig.apply(this, arguments).then(function(response) {
                                if (typeof url === 'string' && url.indexOf('$TOKEN_PATH') !== -1) {
                                    response.clone().text().then(function(body) {
                                        try { MusicxTokenBridge.onTokenBody(body); } catch(e) {}
                                    });
                                }
                                return response;
                            });
                        };
                    })();
                    """.trimIndent(),
                    null,
                )
            }
        }
        return wv
    }

    private fun destroyWebView(wv: WebView) {
        wv.stopLoading()
        wv.removeJavascriptInterface("MusicxTokenBridge")
        wv.loadUrl("about:blank")
        wv.clearHistory()
        wv.clearCache(true)
        android.webkit.WebStorage.getInstance().deleteAllData()
        wv.destroy()
    }

    private class TokenBridge(private val deferred: CompletableDeferred<String>) {
        @JavascriptInterface
        fun onTokenBody(body: String) {
            deferred.complete(body)
        }
    }
}

/**
 * Login WebView navigation policy. Only Spotify and known identity provider
 * origins are allowed; all other top-level navigations are refused.
 */
fun isSpotifyLoginNavigationAllowed(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = try { Uri.parse(url) } catch (_: Exception) { return false }
    if (uri.scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false
    return SpotifyWebTokenHarvester.LOGIN_ALLOWED_HOSTS.any { allowed ->
        host == allowed || host.endsWith(".$allowed")
    }
}
