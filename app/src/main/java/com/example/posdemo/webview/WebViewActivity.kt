package com.example.posdemo.webview

import android.device.DeviceManager
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posdemo.R
import com.example.posdemo.others.KioskRelatedActivity
import com.example.posdemo.others.isOnScreenButtons

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        Log.e("Patrick", intent?.getStringExtra("url") ?: "")
        // For delaying the webView
        webView.loadUrl(intent?.getStringExtra("url") ?: "")

    }

    override fun onStart() {
        super.onStart()
        DeviceManager().enableHomeKey(false)
        DeviceManager().enableStatusBar(false)
    }

    override fun onStop() {
        super.onStop()
        DeviceManager().enableHomeKey(true)
        DeviceManager().enableStatusBar(true)
    }

    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webView)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}