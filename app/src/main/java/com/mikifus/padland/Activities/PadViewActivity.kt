package com.mikifus.padland.Activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.mikifus.padland.Database.PadModel.Pad
import com.mikifus.padland.Database.PadModel.PadViewModel
import com.mikifus.padland.Database.ServerModel.ServerViewModel
import com.mikifus.padland.Dialogs.Managers.IManagesDetectedPadDialog
import com.mikifus.padland.Dialogs.Managers.IManagesNewPadDialog
import com.mikifus.padland.Dialogs.Managers.IManagesNewServerDialog
import com.mikifus.padland.Dialogs.Managers.IManagesPadViewAuthDialog
import com.mikifus.padland.Dialogs.Managers.IManagesSslErrorDialog
import com.mikifus.padland.Dialogs.Managers.IManagesWhitelistServerDialog
import com.mikifus.padland.Dialogs.Managers.ManagesDetectedPadDialog
import com.mikifus.padland.Dialogs.Managers.ManagesNewPadDialog
import com.mikifus.padland.Dialogs.Managers.ManagesNewServerDialog
import com.mikifus.padland.Dialogs.Managers.ManagesPadViewAuthDialog
import com.mikifus.padland.Dialogs.Managers.ManagesSslErrorDialog
import com.mikifus.padland.Dialogs.Managers.ManagesWhitelistServerDialog
import com.mikifus.padland.R
import com.mikifus.padland.Utils.CryptPad.CryptPadUtils
import com.mikifus.padland.Utils.PadLandWebViewClient.PadLandWebClientCallbacks
import com.mikifus.padland.Utils.PadLandWebViewClient.PadLandWebViewClient
import com.mikifus.padland.Utils.PadServer
import com.mikifus.padland.Utils.PadUrl
import com.mikifus.padland.Utils.WhiteListMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PadViewActivity :
    AppCompatActivity(),
    IManagesPadViewAuthDialog by ManagesPadViewAuthDialog(),
    IManagesWhitelistServerDialog by ManagesWhitelistServerDialog(),
    IManagesNewServerDialog by ManagesNewServerDialog() ,
    IManagesSslErrorDialog by ManagesSslErrorDialog(),
    IManagesNewPadDialog by ManagesNewPadDialog(),
    IManagesDetectedPadDialog by ManagesDetectedPadDialog() {

    override var serverViewModel: ServerViewModel? = null
    private var webView: WebView? = null
    private var webViewClient: PadLandWebViewClient? = null
    private var deferredSave: Boolean = false

    private var currentUrl: String? = null
        get() {
            return webView?.url
        }
        set(value) {
            lifecycleScope.launch(Dispatchers.IO) {
                value?.let { loadUrl(value) }
            }
            field = value
        }

    private var mProgressBar: ProgressBar? = null

    @Suppress("DEPRECATION")
    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                    }
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    return true
                }
            }
            return false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.extras == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_pad_view)

        val root = findViewById<View>(R.id.activity_pad_view_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        loadProgress()
        showProgress()
        initViewModels()
    }

    private fun makeWebView(urlWhitelist: List<String>) {
        if(!isNetworkAvailable) {
            Toast.makeText(applicationContext, getString(R.string.network_is_unreachable), Toast.LENGTH_LONG)
                .show()
            return
        }

        val activity = this
        webViewClient = PadLandWebViewClient(urlWhitelist, object: PadLandWebClientCallbacks {
            override fun onStartLoading() {
                showProgress()
            }

            override fun onStopLoading() {
                hideProgress()
            }

            override suspend fun onUnsafeUrlProtocol(url: String): Boolean {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PadViewActivity,
                        "NEXUS blocked an unsafe cleartext URL.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return false
            }

            override suspend fun onExternalHostUrlLoad(url: String): Boolean {
                showWhitelistServerDialog(this@PadViewActivity, url,
                    { dialogUrl ->
                        showNewServerDialog(this@PadViewActivity, dialogUrl) {
                            whitelistUrl(dialogUrl)
                            loadUrl(dialogUrl)
                        }
                    },{
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        if(webView?.canGoBack() == true) {
                            webView!!.goBack()
                        } else {
                            finish()
                        }
                    },
                    {
                        // Cancel: do not whitelist or load an unapproved host.
                    }
                )
                return false
            }

            override fun onReceivedSslError(handler: SslErrorHandler, url: String, message: String) {
                // Defensive duplicate of the fail-closed WebViewClient policy.
                handler.cancel()
            }

            override fun onReceivedHttpAuthRequestCallback(
                view: WebView,
                handler: HttpAuthHandler,
                host: String,
                realm: String
            ) {
                showPadViewAuthDialog(this@PadViewActivity, view, handler)
            }

            override fun onPageFinishedCallback(view: WebView?, url: String?) {
                if(url.isNullOrBlank()) {
                    return
                }
                Pad.fromUrl(url, activity).value!!

                if (!deferredSave && CryptPadUtils.seemsCrpytPadUrl(url)) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val pad = withContext(Dispatchers.IO) {
                            padViewModel?.getByUrl(url)
                        }
                        if(pad == null &&
                            webViewClient!!.hostsWhitelist.contains(
                                PadServer.Builder().padUrl(url, activity).host
                            )) {
                            showDetectedPadDialog(activity, url,
                                {
                                    // Do nothing
                                },
                                {
                                    savePadFromUrl(url)
                                }
                            )
                        }
                    }
                } else if (deferredSave && WhiteListMatcher.isValidHost(url, webViewClient!!.hostsWhitelist) && CryptPadUtils.seemsCrpytPadUrl(url)) {
                    deferredSave = false
                    savePadFromUrl(url)
                }
            }
        })

        makeWebSettings()
    }

    private fun initViewModels() {
        if(padViewModel == null) {
            padViewModel = ViewModelProvider(this)[PadViewModel::class.java]
        }

        if(serverViewModel == null) {
            serverViewModel = ViewModelProvider(this)[ServerViewModel::class.java]
        }

        serverViewModel?.getAllEnabled!!.observe(this) { servers ->
            val serverList = servers.map {
                    URL(it.mUrl).host
                } + resources.getStringArray(R.array.etherpad_servers_whitelist)

            makeWebView(serverList)
            loadOrSavePad()
        }
    }

    private fun loadOrSavePad() {
        if(!isNetworkAvailable) {
            finish()
            return
        }

        if(intent.extras?.containsKey("padId") == true) {
            loadPadById(intent!!.extras!!.getLong("padId"))
            return
        }

        val padUrl = intent.extras!!.getString("android.intent.extra.TEXT")

        if(padUrl.isNullOrBlank()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.unexpected_error),
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val userDetails =
            getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)
        var save = userDetails.getBoolean("auto_save_new_pads", true)
        if(intent.extras?.containsKey("padUrlDontSave") == true
            && intent.extras!!.getBoolean("padUrlDontSave")) {
            save = false
        } else if(intent.extras?.containsKey("deferredSave") == true
            && intent.extras!!.getBoolean("deferredSave")) {
            this.deferredSave = true
            save = false
        }
        currentUrl = padUrl

        if(save) {
            savePadFromUrl(padUrl)
        }
    }

    private fun loadPadById(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pad = padViewModel?.getById(id)

            if (pad != null) {
                currentUrl = pad.mUrl
                updateViewedPad(pad)
            } else {
                lifecycleScope.launch {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.unexpected_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun savePadFromUrl(padUrl: String) {
        val activity = this
        val localName = intent.extras?.getString("localName", "") ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            val pad = withContext(Dispatchers.IO){
                padViewModel?.getByUrl(padUrl)
            }

            if(pad == null &&
                webViewClient!!.hostsWhitelist.contains(
                    PadServer.Builder().padUrl(padUrl, activity).host
                )) {

                val newPad = withContext(Dispatchers.Main){
                    Pad.fromUrl(padUrl, activity).value!!.copy(mLocalName = localName)
                }

                withContext(Dispatchers.IO){
                    padViewModel!!.insertPad(newPad)
                    updateViewedPad(newPad)
                }

                withContext(Dispatchers.Main){
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.padview_pad_save_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun updateViewedPad(pad: Pad) {
        val updatedPad = pad.copy(
            mAccessCount = pad.mAccessCount + 1,
            mLastUsedDate = java.sql.Date(System.currentTimeMillis())
        )

        padViewModel?.updatePad(updatedPad)
    }

    private fun loadProgress() {
        mProgressBar = findViewById(R.id.progress_indicator)
    }

    fun showProgress() {
        mProgressBar!!.visibility = View.VISIBLE
    }

    fun hideProgress() {
        mProgressBar!!.visibility = View.GONE
    }

    private fun loadUrl(url: String) {
        if (!WhiteListMatcher.isValidHost(url, webViewClient!!.hostsWhitelist)) {
            showWhitelistServerDialog(this, url,
                { dialogUrl ->
                    showNewServerDialog(this, dialogUrl){
                        whitelistUrl(dialogUrl)
                        loadUrl(dialogUrl)
                    }
                },
                { dialogUrl ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dialogUrl))
                    startActivity(intent)
                    if(webView?.canGoBack() == true) {
                        webView!!.goBack()
                    } else {
                        finish()
                    }
                },
                {
                    // Cancel: do not whitelist or load an unapproved host.
                }
            )
            return
        }

        val userDetails =
            getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        val username = userDetails.getString("padland_default_username", "")
        val color = userDetails.getInt("padland_default_color", 0)

        val newUrl = PadUrl.etherpadAddUsernameAndColor(url, username, color)

        lifecycleScope.launch(Dispatchers.Main) {
            webView!!.loadUrl(newUrl)
        }
    }

    fun whitelistUrl(url: String) {
        val urlObject = URL(url)
        webViewClient!!.hostsWhitelist += urlObject.host
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun makeWebSettings() {
        webView = findViewById(R.id.activity_main_webview)
        webView!!.webViewClient = webViewClient!!
        webView!!.setInitialScale(1)

        val webSettings = webView!!.settings

        webSettings.javaScriptEnabled = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true

        // NEXUS privacy/security defaults.
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = false
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webSettings.saveFormData = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            webSettings.isAlgorithmicDarkeningAllowed = true
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_ON)
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    WebSettingsCompat.setForceDark(webSettings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, false)
        }

        webView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View, keyCode: Int, keyEvent: KeyEvent): Boolean {
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    val webView = view as WebView
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> if (webView.canGoBack()) {
                            webView.goBack()
                            return true
                        }
                    }
                }
                return false
            }
        })
    }
}
