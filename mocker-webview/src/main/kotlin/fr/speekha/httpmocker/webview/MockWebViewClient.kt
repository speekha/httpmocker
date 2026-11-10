/*
 * Copyright 2019-2026 David Blanc
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

package fr.speekha.httpmocker.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.getLogger
import fr.speekha.httpmocker.io.MockResponder
import fr.speekha.httpmocker.okhttp.MockResponseInterceptor
import fr.speekha.httpmocker.webview.io.toGenericModel
import fr.speekha.httpmocker.webview.io.toWebResourceResponse
import kotlinx.coroutines.runBlocking

/**
 * A WebViewClient that intercepts requests made by a WebView and answers them with predefined
 * responses, enabling offline usage of web content.
 *
 * Shares the same scenario providers and delay as the given [interceptor], so both OkHttp calls
 * and WebView requests are mocked from a single configuration.
 */
class MockWebViewClient(interceptor: MockResponseInterceptor) : WebViewClient(), MockResponseInterceptor.OnModeChangeListener {

    /**
     * Enables to set the interception mode.
     * @see fr.speekha.httpmocker.Mode
     */
    internal var mode: Mode by interceptor::mode

    private val logger = getLogger()

    private val responder = MockResponder<WebResourceRequest, WebResourceResponse?>(
        interceptor.providers,
        interceptor.delay,
        { it.toGenericModel() },
        { _, descriptor -> descriptor.toWebResourceResponse() }
    )

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        logger.info("Intercepted WebView request ${request.url}: client is $mode")
        return when (mode) {
            Mode.DISABLED -> super.shouldInterceptRequest(view, request)
            Mode.ENABLED -> runBlocking { responder.mockResponse(request) }
            Mode.MIXED -> runBlocking { responder.mockResponseOrNull(request) } ?: super.shouldInterceptRequest(view, request)
            Mode.RECORD -> super.shouldInterceptRequest(view, request)
        }
    }

    override fun onModeChanged(newMode: Mode) {
        mode = newMode
    }
}
