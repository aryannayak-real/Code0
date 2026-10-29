package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePreviewSheet(
    fileName: String,
    fileExtension: String,
    content: String,
    projectDir: File,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Preview — $fileName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Local Web & Markdown Sandbox",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("preview_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }
            }

            // WebView Preview Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                LiveWebContentView(
                    content = content,
                    extension = fileExtension.lowercase(),
                    projectDir = projectDir
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebContentView(
    content: String,
    extension: String,
    projectDir: File
) {
    val preparedHtml = remember(content, extension) {
        when (extension) {
            "html", "htm" -> content
            "md", "markdown" -> convertMarkdownToHtml(content)
            "svg" -> """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { margin: 0; padding: 16px; display: flex; justify-content: center; align-items: center; min-height: 90vh; background: #1e1e1e; }
                        svg { max-width: 100%; height: auto; }
                    </style>
                </head>
                <body>
                    $content
                </body>
                </html>
            """.trimIndent()
            else -> """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: monospace; padding: 16px; background: #121212; color: #e0e0e0; white-space: pre-wrap; word-break: break-all; }
                    </style>
                </head>
                <body>${escapeHtml(content)}</body>
                </html>
            """.trimIndent()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE

                val baseUrl = if (projectDir.exists()) "file://${projectDir.absolutePath}/" else null
                loadDataWithBaseURL(baseUrl, preparedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val baseUrl = if (projectDir.exists()) "file://${projectDir.absolutePath}/" else null
            webView.loadDataWithBaseURL(baseUrl, preparedHtml, "text/html", "UTF-8", null)
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_webview_preview")
    )
}

/**
 * Clean built-in Markdown to HTML parser
 */
fun convertMarkdownToHtml(markdown: String): String {
    val sb = StringBuilder()
    sb.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    padding: 16px;
                    line-height: 1.6;
                    color: #d4d4d4;
                    background-color: #1e1e1e;
                }
                h1, h2, h3, h4 { color: #ffffff; margin-top: 24px; margin-bottom: 12px; }
                h1 { border-bottom: 1px solid #333333; padding-bottom: 8px; }
                h2 { border-bottom: 1px solid #2a2a2a; padding-bottom: 6px; }
                code {
                    font-family: monospace;
                    background: #2d2d2d;
                    padding: 2px 5px;
                    border-radius: 4px;
                    color: #9cdcfe;
                    font-size: 13px;
                }
                pre {
                    background: #252526;
                    padding: 12px;
                    border-radius: 6px;
                    overflow-x: auto;
                    border: 1px solid #3c3c3c;
                }
                pre code { background: none; padding: 0; }
                blockquote {
                    border-left: 4px solid #007acc;
                    margin: 0;
                    padding-left: 12px;
                    color: #9e9e9e;
                }
                ul, ol { padding-left: 20px; }
                li { margin-bottom: 4px; }
                hr { border: none; border-top: 1px solid #3a3a3a; margin: 20px 0; }
                a { color: #4ec9b0; text-decoration: none; }
                table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                th, td { border: 1px solid #3a3a3a; padding: 8px 12px; text-align: left; }
                th { background-color: #2a2a2a; color: #fff; }
            </style>
        </head>
        <body>
    """.trimIndent())

    var inCodeBlock = false
    var inList = false

    for (line in markdown.lines()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                sb.append("</code></pre>\n")
                inCodeBlock = false
            } else {
                sb.append("<pre><code>")
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            sb.append(escapeHtml(line)).append("\n")
            continue
        }

        if (trimmed.startsWith("# ")) {
            sb.append("<h1>").append(formatInlineMarkdown(trimmed.substring(2))).append("</h1>\n")
        } else if (trimmed.startsWith("## ")) {
            sb.append("<h2>").append(formatInlineMarkdown(trimmed.substring(3))).append("</h2>\n")
        } else if (trimmed.startsWith("### ")) {
            sb.append("<h3>").append(formatInlineMarkdown(trimmed.substring(4))).append("</h3>\n")
        } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            if (!inList) {
                sb.append("<ul>\n")
                inList = true
            }
            sb.append("<li>").append(formatInlineMarkdown(trimmed.substring(2))).append("</li>\n")
        } else if (trimmed.startsWith("> ")) {
            sb.append("<blockquote>").append(formatInlineMarkdown(trimmed.substring(2))).append("</blockquote>\n")
        } else if (trimmed.isEmpty()) {
            if (inList) {
                sb.append("</ul>\n")
                inList = false
            }
            sb.append("<br/>\n")
        } else {
            if (inList) {
                sb.append("</ul>\n")
                inList = false
            }
            sb.append("<p>").append(formatInlineMarkdown(line)).append("</p>\n")
        }
    }

    if (inList) sb.append("</ul>\n")
    if (inCodeBlock) sb.append("</code></pre>\n")

    sb.append("</body></html>")
    return sb.toString()
}

private fun formatInlineMarkdown(text: String): String {
    var out = escapeHtml(text)
    // bold **text**
    out = out.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
    // inline code `code`
    out = out.replace(Regex("`(.*?)`"), "<code>$1</code>")
    // italics *text*
    out = out.replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
    return out
}

private fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
