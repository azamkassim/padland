package com.mikifus.padland.Utils.PadLandWebViewClient

import android.graphics.Bitmap
import android.os.Build
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mikifus.padland.Utils.WhiteListMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * WebView client with host whitelisting and NEXUS transport enforcement.
 *
 * Remote traffic must use HTTPS. Cleartext HTTP is permitted only for
 * same-device loopback hosts so a local Termux-hosted service can be used.
 */
open class PadLandSaferWebViewClient(var hostsWhitelist: List<String>) : WebViewClient() {
    private var corsDomains: List<String>? = null
    private val webResourceResponseFromString: WebResourceResponse
        get() = getUtf8EncodedWebResourceResponse(ByteArrayInputStream("".toByteArray()))

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return if (isValidRequestUrl(request.url.toString())) {
            super.shouldInterceptRequest(view, request)
        } else {
            webResourceResponseFromString
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        return if (isValidRequestUrl(url)) {
            @Suppress("DEPRECATION")
            super.shouldInterceptRequest(view, url)
        } else {
            webResourceResponseFromString
        }
    }

    private fun getUtf8EncodedWebResourceResponse(data: ByteArrayInputStream?): WebResourceResponse {
        return WebResourceResponse("text/css", "UTF-8", data)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return false
    }

    @Deprecated("Deprecated in Java", ReplaceWith("shouldOverrideUrlLoading(view, request)"))
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return false
    }

    private fun isValidRequestUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val isHttps = URLUtil.isHttpsUrl(url)
        val isLocalHttp = URLUtil.isHttpUrl(url) && isLoopbackUrl(url)

        if (!isHttps && !isLocalHttp) {
            return false
        }

        val hostsList: List<String> = corsDomains?.let { hostsWhitelist + it } ?: hostsWhitelist
        return WhiteListMatcher.isValidHost(url, hostsList)
    }

    private fun isLoopbackUrl(url: String): Boolean {
        val host = runCatching { URL(url).host.lowercase() }.getOrNull() ?: return false
        return host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]"
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (corsDomains == null && view != null && isValidRequestUrl(url)) {
            corsDomains = listOf()
            view.findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                val headers = url?.let { getUrlHeaders(it) }
                if (headers != null &&
                    headers.containsKey("content-security-policy") &&
                    headers["content-security-policy"]!!.isNotEmpty()) {
                    corsDomains = extractUniqueUrls(headers["content-security-policy"]!![0])
                }
            }
        }
        super.onPageStarted(view, url, favicon)
    }

    private fun getUrlHeaders(url: String): Map<String, List<String>> {
        var headers = mutableMapOf<String, List<String>>()
        val connectionUrl = URL(url)
        val connection = connectionUrl.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                headers = connection.headerFields
            }
        } finally {
            connection.disconnect()
        }
        return headers
    }

    private fun extractUniqueUrls(input: String): List<String> {
        val urlRegex = Regex("""https?:\/\/([^\s;]+)""")
        return urlRegex.findAll(input)
            .map { it.groups[1]!!.value }
            .toSet()
            .toList()
    }
}
