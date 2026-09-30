package cooking.fifi.android.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cooking.fifi.android.data.S
import cooking.fifi.android.data.VideoItem
import cooking.fifi.android.ui.LocalStrings
import cooking.fifi.android.ui.theme.Palette
import cooking.fifi.android.ui.theme.TS
import cooking.fifi.android.ui.theme.W
import cooking.fifi.android.ui.theme.fifi

/**
 * Full-screen YouTube player: privacy-enhanced youtube-nocookie.com embed in
 * a locked-down WebView (only the player loads in-app; any other navigation
 * opens externally). Pauses with the activity (standby, app switch) and is
 * destroyed on dismiss so audio never outlives the dialog.
 */
@Composable
fun VideoDialog(video: VideoItem, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val watchUrl = "https://www.youtube.com/watch?v=${video.id}"
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("videoDialog")) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, s[S.close], tint = Color.White) }
                    Text(video.title, style = fifi(TS.Headline, W.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, watchUrl.toUri())) } catch (_: ActivityNotFoundException) {}
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Palette.sun)
                        Text(" " + s[S.openInYouTube], style = fifi(TS.Subheadline, W.Bold), color = Palette.sun)
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    YouTubeWebView(video.id, Modifier.widthIn(max = 1280.dp).fillMaxWidth().aspectRatio(16f / 9f))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebView(videoId: String, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val web = remember(videoId) {
        WebView(context).apply {
            // AndroidView defaults to WRAP_CONTENT, which makes the page's
            // viewport height 0 (100vh / 100% iframes collapse). Fill the slot.
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                // Keep the WebView to the player only; links (channel, "watch on
                // YouTube") open in the YouTube app / browser.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val host = request.url.host.orEmpty()
                    if (host.endsWith("youtube-nocookie.com") && request.url.path.orEmpty().startsWith("/embed/")) return false
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) } catch (_: ActivityNotFoundException) {}
                    return true
                }
            }
            webChromeClient = FullscreenChromeClient(this)
            // The embed needs an HTTP referrer (YouTube error 153 without one),
            // so the iframe is hosted on a page whose base URL is our site.
            val html = """
                <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                <style>html,body{margin:0;padding:0;background:#000;overflow:hidden}iframe{position:fixed;inset:0;border:0;width:100vw;height:100vh}</style></head>
                <body><iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&playsinline=1&modestbranding=1"
                allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen
                referrerpolicy="strict-origin-when-cross-origin"></iframe></body></html>
            """.trimIndent()
            loadDataWithBaseURL("https://android.fifi.cooking/", html, "text/html", "utf-8", null)
        }
    }
    // Pause/resume with the host lifecycle (standby, app switch). Keyed on the
    // lifecycle, which can change once the dialog window attaches — so this
    // effect must never tear the WebView down.
    DisposableEffect(lifecycle, web) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> web.onPause()
                Lifecycle.Event.ON_RESUME -> web.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    // Destroy only when the player itself leaves composition (dialog closed).
    DisposableEffect(web) {
        onDispose {
            web.stopLoading()
            web.loadUrl("about:blank")
            web.destroy()
        }
    }
    AndroidView(factory = { web }, modifier = modifier)
}

/** Lets the embed's fullscreen button take over the dialog window. */
private class FullscreenChromeClient(private val web: WebView) : WebChromeClient() {
    private var custom: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, cb: CustomViewCallback) {
        val root = web.rootView as? ViewGroup ?: return cb.onCustomViewHidden()
        custom = view
        callback = cb
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun onHideCustomView() {
        (web.rootView as? ViewGroup)?.removeView(custom)
        custom = null
        callback?.onCustomViewHidden()
        callback = null
    }
}
