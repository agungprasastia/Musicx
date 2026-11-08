package com.example.musicx.spotify.auth

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Offscreen WebView that loads `open.spotify.com` with the stored `sp_dc`
 * cookie, waits for the page's own `/api/token` fetch to complete, and
 * captures the response body through a narrow native bridge.
 */
class SpotifyWebTokenHarvester(private val context: Context) {

    suspend fun harvest(secret: SpotifySecret): SpotifyWebToken =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String>()
            var webView: WebView? = null
            try {
                injectCookie(secret)
                webView = createHarvestWebView(context, deferred)
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
        private const val PLAYER_URL = "https://open.spotify.com/"
        private const val HARVEST_TIMEOUT_MS = 25_000L
        private const val BRIDGE_NAME = "MusicxTokenBridge"
        private const val CANVAS_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        val LOGIN_ALLOWED_HOSTS = setOf(
            "spotify.com",
            "scdn.co",
            "spotifycdn.com",
            "accounts.google.com",
            "appleid.apple.com",
            "facebook.com",
        )

        private val HOOK_SCRIPT = """
            (function () {
              if (window.__musicxTokenHook) return;
              window.__musicxTokenHook = true;
              var report = function (body) {
                try { $BRIDGE_NAME.onTokenBody(body); } catch (e) {}
              };
              var isToken = function (u) {
                try { return String(u).indexOf('/api/token') !== -1; } catch (e) { return false; }
              };
              var origFetch = window.fetch;
              if (origFetch) {
                window.fetch = function (input, init) {
                  var url = (input && input.url) ? input.url : input;
                  var result = origFetch.apply(this, arguments);
                  if (isToken(url)) {
                    try {
                      result.then(function (res) {
                        res.clone().text().then(report).catch(function () {});
                      }).catch(function () {});
                    } catch (e) {}
                  }
                  return result;
                };
              }
              var origOpen = XMLHttpRequest.prototype.open;
              XMLHttpRequest.prototype.open = function (method, url) {
                this.__musicxUrl = url;
                return origOpen.apply(this, arguments);
              };
              var origSend = XMLHttpRequest.prototype.send;
              XMLHttpRequest.prototype.send = function () {
                var xhr = this;
                try {
                  xhr.addEventListener('load', function () {
                    if (isToken(xhr.__musicxUrl)) {
                      try { report(xhr.responseText); } catch (e) {}
                    }
                  });
                } catch (e) {}
                return origSend.apply(this, arguments);
              };
            })();
        """.trimIndent()
    }

    private fun injectCookie(secret: SpotifySecret) {
        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://open.spotify.com/", "sp_dc=${secret.reveal()}; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://accounts.spotify.com/", "sp_dc=${secret.reveal()}; Domain=.spotify.com; Path=/; Secure")
            flush()
        }
        runCatching { WebStorage.getInstance().deleteAllData() }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createHarvestWebView(ctx: Context, deferred: CompletableDeferred<String>): WebView {
        val wv = WebView(ctx.applicationContext)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = CANVAS_UA
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        wv.addJavascriptInterface(TokenBridge(deferred), BRIDGE_NAME)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(HOOK_SCRIPT, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(HOOK_SCRIPT, null)
            }
        }
        return wv
    }

    private fun destroyWebView(wv: WebView) {
        runCatching {
            wv.stopLoading()
            wv.removeJavascriptInterface(BRIDGE_NAME)
            wv.loadUrl("about:blank")
            wv.clearHistory()
            wv.clearCache(true)
            wv.destroy()
        }
    }

    private class TokenBridge(private val deferred: CompletableDeferred<String>) {
        private val json = Json { ignoreUnknownKeys = true }

        @JavascriptInterface
        fun onTokenBody(body: String?) {
            if (body.isNullOrBlank() || deferred.isCompleted) return
            try {
                val root = json.parseToJsonElement(body).jsonObject
                val token = root["accessToken"]?.jsonPrimitive?.contentOrNull
                val anonymous = root["isAnonymous"]?.jsonPrimitive?.booleanOrNull ?: false
                if (token.isNullOrBlank() || anonymous) return
                deferred.complete(body)
            } catch (_: Exception) {
                // If parsing check fails, ignore and keep waiting for valid token
            }
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
