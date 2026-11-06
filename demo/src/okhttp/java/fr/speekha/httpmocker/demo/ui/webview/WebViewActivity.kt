/*
 * Copyright 2019-2021 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.demo.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.webview.MockWebViewClient
import org.koin.android.ext.android.inject

class WebViewActivity : AppCompatActivity() {

    private val webViewClient: MockWebViewClient by inject()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val mode = intent.getSerializableExtra(EXTRA_MODE) as? Mode ?: Mode.DISABLED
        webViewClient.mode = mode

        findViewById<WebView>(R.id.webview).apply {
            settings.javaScriptEnabled = true
            webViewClient = this@WebViewActivity.webViewClient
            loadUrl(KOTLIN_GITHUB_URL)
        }
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val KOTLIN_GITHUB_URL = "https://github.com/kotlin"

        fun start(context: Context, mode: Mode) {
            context.startActivity(
                Intent(context, WebViewActivity::class.java)
                    .putExtra(EXTRA_MODE, mode)
            )
        }
    }
}
