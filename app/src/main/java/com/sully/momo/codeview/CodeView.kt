package com.sully.momo.codeview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger


class CodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var code = ""
    private lateinit var languageName: String

    init {
        setPadding(0, 0, 0, 0)
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                execJS("document.getElementsByTagName('html')[0].innerHTML)") {
                    Timber.tag(TAG).d(it)
                }
                super.onPageFinished(view, url)
            }
        }
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.loadWithOverviewMode = true

        addJavascriptInterface(InjectObj(), "InjectObj")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    private val seq = AtomicInteger(0)
    private val callbacks = mutableMapOf<Int, (String) -> Unit>()

    @SuppressLint("ObsoleteSdkInt")
    private fun execJS(js: String, callback: (String) -> Unit) {
        val callId = seq.getAndIncrement()
        callbacks[callId] = callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("javascript:window.InjectObj.callback($callId, $js);", null)
        } else {
            loadUrl("javascript:window.InjectObj.callback($callId, $js);")
        }

    }


    private inner class InjectObj {
        @JavascriptInterface
        fun callback(callId: Int, result: String) {
            callbacks.remove(callId)?.invoke(result)
        }
    }

    fun setCode(code: String, language: String) {
        this.code = code
        this.languageName = language
        loadDataWithBaseURL(
            "",
            toHtml(),
            "text/html",
            "UTF-8",
            ""
        )
    }

    private fun toHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
                <head>
                    <link rel="stylesheet" href="file:///android_asset/highlightjs/styles/darcula.css">
                    <style>
                        body {
                            font-size:").append(String.format("%dpx;", (int) getFontSize()
                            padding: 0px; margin: 0px; line-height: 1.2;
                        }
                        pre { margin: 0px; padding: 0px; position: relative; }
                        table, td, tr { margin: 0px; padding: 0px; }
                    </style>
                    <script src="file:///android_asset/highlightjs/highlight.pack.js"></script>
                    <script>hljs.initHighlightingOnLoad();</script>
                </head>
                <body>
                    <pre><code class='${languageName}'/>$code</pre>
                </body>
                </html>
        """.trimIndent()
    }


    companion object {
        const val TAG = "CodeView"
    }
}