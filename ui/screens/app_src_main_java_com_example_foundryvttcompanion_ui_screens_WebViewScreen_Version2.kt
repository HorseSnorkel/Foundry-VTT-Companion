package com.example.foundryvttcompanion.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import com.example.foundryvttcompanion.viewmodel.SessionViewModel
import java.net.URLEncoder

class FoundryWebViewController {
    internal var webView: WebView? = null

    fun sendChat(content: String) {
        val esc = URLEncoder.encode(content, "UTF-8")
        // Call into injected bridge to send via Foundry client
        webView?.evaluateJavascript("window.FVTT_ANDROID && window.FVTT_ANDROID.sendChat(decodeURIComponent('$esc'));", null)
    }
}

private class FoundryJsBridge(
    private val onChat: (author: String, content: String, tsMillis: Long) -> Unit
) {
    @JavascriptInterface
    fun onChatMessage(author: String, content: String, timestamp: Long) {
        onChat(author, content, timestamp)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FoundryWebView(
    url: String,
    controller: FoundryWebViewController,
    onChatMessage: (author: String, content: String, tsMillis: Long) -> Unit
) {
    val bridge = remember { FoundryJsBridge(onChatMessage) }

    AndroidView(factory = { context ->
        WebView(context).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(bridge, "Android")
            controller.webView = this
            loadUrl(url)
        }
    }, update = { web ->
        // Inject the JS bridge after page loads enough; simple retry on each update
        web.evaluateJavascript(BridgeLoader.loaderJs, null)
    })

    DisposableEffect(Unit) {
        onDispose {
            controller.webView = null
        }
    }
}

private object BridgeLoader {
    // Loads the asset into the page; safe to call multiple times
    val loaderJs: String by lazy {
        """
        (function(){
          if (window.__FVTT_ANDROID_BRIDGE_INSTALLED__) return;
          window.__FVTT_ANDROID_BRIDGE_INSTALLED__ = true;

          // Create namespace
          window.FVTT_ANDROID = window.FVTT_ANDROID || {};

          // Outbound: Android -> Foundry
          window.FVTT_ANDROID.sendChat = function(text) {
            try {
              if (typeof ChatMessage !== 'undefined' && ChatMessage.create) {
                ChatMessage.create({content: text});
              } else if (window.ui && ui.chat && ui.chat.submitMessage) {
                ui.chat.submitMessage({content: text});
              } else {
                console.warn('FVTT bridge: cannot find chat sender.');
              }
            } catch(e) {
              console.error('FVTT bridge sendChat error', e);
            }
          };

          // Inbound: Foundry -> Android
          function notifyAndroid(author, content, ts) {
            try {
              if (window.Android && Android.onChatMessage) {
                Android.onChatMessage(String(author||'Unknown'), String(content||''), Number(ts||Date.now()));
              }
            } catch(e) {
              console.error('FVTT bridge Android dispatch error', e);
            }
          }

          function wireHooks(){
            try {
              if (window.Hooks && Hooks.on) {
                // When a chat message is created (document-level)
                Hooks.on('createChatMessage', (doc)=>{
                  try {
                    const user = game?.users?.get(doc.user) || game?.user;
                    notifyAndroid(user?.name || 'User', doc.content, (doc.timestamp||Date.now()));
                  } catch(e) { console.error(e); }
                });
                // Also when rendered (covers older cases)
                Hooks.on('renderChatMessage', (msg, html, data)=>{
                  try {
                    const author = data?.message?.speaker?.alias || data?.user?.name || 'User';
                    const content = data?.message?.content || '';
                    notifyAndroid(author, content, Date.now());
                  } catch(e) { console.error(e); }
                });
              } else {
                // Retry later
                setTimeout(wireHooks, 1500);
              }
            } catch(e) {
              setTimeout(wireHooks, 1500);
            }
          }
          wireHooks();
        })();
        """.trimIndent()
    }
}