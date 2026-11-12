package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.GlassActionPill
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPanel
import com.example.ui.icons.LineIconType
import com.example.ui.theme.LocalGlassTokens

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SingleFileWebScreen(
    onBack: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("single_file_web_screen")
    ) {
        GlassPanel(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        icon = LineIconType.BACK,
                        contentDesc = "Back to Native App",
                        sizeDp = 44.dp,
                        onClick = onBack,
                        testTagName = "web_screen_back_button"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Single-File HTML (<100KB)",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                GlassActionPill(
                    label = "Copy HTML",
                    icon = LineIconType.COPY,
                    primary = true,
                    onClick = {
                        try {
                            val html = context.assets.open("glassnotes.html").bufferedReader().use { it.readText() }
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("glassnotes.html", html))
                            onShowToast("Copied self-contained glassnotes.html (${html.toByteArray().size / 1024} KB)")
                        } catch (e: Exception) {
                            onShowToast("Failed to copy HTML")
                        }
                    },
                    modifier = Modifier.testTag("copy_single_file_html_button")
                )
            }
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
                        }
                    }
                    loadUrl("file:///android_asset/glassnotes.html")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
