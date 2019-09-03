/*
 * Copyright 2019 David Blanc
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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.RequestRecorder.CallRecord
import fr.speekha.httpmocker.model.ResponseDescriptor
import fr.speekha.httpmocker.policies.FilingPolicy
import fr.speekha.httpmocker.policies.MirrorPathPolicy
import fr.speekha.httpmocker.scenario.DynamicMockProvider
import fr.speekha.httpmocker.scenario.RequestCallback
import fr.speekha.httpmocker.scenario.ScenarioProvider
import fr.speekha.httpmocker.scenario.StaticMockProvider
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.InputStream

/**
 * A OkHTTP interceptor that can let requests through or block them and answer them with predefined responses.
 * Genuine network connections can also be recorded to create reusable offline scenarios.
 */
class MockResponseInterceptor
private constructor(
    private var providers: List<ScenarioProvider>,
    private var requestRecorder: RequestRecorder?
) : Interceptor {

    /**
     * An arbitrary delay to include when answering requests in order to have a realistic behavior (GUI can display
     * loaders, etc.)
     */
    private var delay: Long = 0

    /**
     * Enables to set the interception mode. @see fr.speekha.httpmocker.MockResponseInterceptor.Mode
     */
    var mode: Mode = Mode.DISABLED
        set(value) {
            if (value == Mode.RECORD && requestRecorder == null) {
                error(NO_RECORDER_ERROR)
            } else {
                field = value
            }
        }

    private val logger = getLogger()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.info("Intercepted request $request: Interceptor is $mode")
        return respondToRequest(chain, request)
    }

    private fun respondToRequest(chain: Interceptor.Chain, request: Request) = when (mode) {
        Mode.DISABLED -> proceedWithNetworkCall(chain)
        Mode.ENABLED -> mockResponse(request) ?: buildResponse(request, responseNotFound(), null)
        Mode.MIXED -> mockResponse(request) ?: proceedWithNetworkCall(chain)
        Mode.RECORD -> recordCall(chain)
    }

    private fun proceedWithNetworkCall(chain: Interceptor.Chain) = chain.proceed(chain.request())

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun mockResponse(request: Request): Response? = providers.asSequence()
        .mapNotNull { provider ->
            logger.info("Looking up mock scenario for $request in $provider")
            try {
                provider.loadResponse(request)
            } catch (e: Throwable) {
                logger.error("Scenario file could not be loaded", e)
                val exceptionType = e.javaClass.name
                val message = e.message
                val stackTrace = e.stackTrace.joinToString("\n\tat ")
                responseNotFound("$exceptionType: $message\n\tat $stackTrace")
            }?.let { response ->
                executeMockResponse(response, request, provider)
            }
        }
        .firstOrNull()

    private fun executeMockResponse(
        response: ResponseDescriptor,
        request: Request,
        provider: ScenarioProvider
    ): Response {
        logger.info("Response was found: $response")
        simulateDelay(response)
        return buildResponse(request, response, provider)
    }

    private fun simulateDelay(response: ResponseDescriptor) {
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
    }

    private fun buildResponse(
        request: Request,
        response: ResponseDescriptor,
        provider: ScenarioProvider?
    ): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(response.code)
        .message(messageForHttpCode(response.code))
        .addHeaders(response)
        .body(loadResponseBody(request, response, provider))
        .build()

    private fun Response.Builder.addHeaders(response: ResponseDescriptor) = apply {
        header("Content-type", response.mediaType)
        response.headers.forEach {
            if (it.value != null) {
                header(it.name, it.value)
            }
        }
    }

    private fun loadResponseBody(
        request: Request,
        response: ResponseDescriptor,
        provider: ScenarioProvider?
    ) = ResponseBody.create(
        MediaType.parse(response.mediaType), response.bodyFile?.let {
            logger.info("Loading response body from file: $it")
            provider?.loadResponseBody(request, it)
        } ?: response.body.toByteArray()
    )

    private fun responseNotFound(body: String = "Page not found") =
        ResponseDescriptor(code = 404, body = body)

    private fun recordCall(chain: Interceptor.Chain): Response = requestRecorder?.run {
        val response = proceedWithNetworkCall(chain)
        val body = response.body()?.bytes()
        val record = CallRecord(chain.request(), response, body)
        saveFiles(record)
        response.copyResponse(body)
    } ?: error(RECORD_NOT_SUPPORTED_ERROR)

    private fun messageForHttpCode(httpCode: Int) =
        HTTP_RESPONSES_CODE[httpCode] ?: "Unknown error code"

    /**
     * Defines the interceptor's state and how it is supposed to respond to requests (intercept
     * them, let them through or record them)
     */
    enum class Mode(private val status: String) {
        /** lets every request through without interception. */
        DISABLED("disabled"),
        /** intercepts all requests and return responses found in a predefined configuration */
        ENABLED("enabled"),
        /** allows to look for responses locally, but execute the request if no response is found */
        MIXED("in mixed mode"),
        /** allows to record actual requests and responses for future use as mock scenarios */
        RECORD("recording");

        override fun toString(): String = status
    }

    /**
     * Builder to instantiate an interceptor.
     */
    data class Builder(
        private var filingPolicy: FilingPolicy = MirrorPathPolicy(),
        private var openFile: LoadFile? = null,
        private var mapper: Mapper? = null,
        private var root: File? = null,
        private var simulatedDelay: Long = 0,
        private var interceptorMode: Mode = Mode.DISABLED,
        private val dynamicCallbacks: MutableList<RequestCallback> = mutableListOf(),
        private var showSavingErrors: Boolean = false
    ) {

        /**
         * For static mocks: Defines the policy used to retrieve the configuration files based
         * on the request being intercepted
         * @param policy the naming policy to use for scenario files
         */
        fun decodeScenarioPathWith(policy: FilingPolicy): Builder = apply { filingPolicy = policy }

        /**
         * For static mocks: Defines the policy used to retrieve the configuration files based
         * on the request being intercepted
         * @param policy a lambda to use as the naming policy for scenario files
         */
        fun decodeScenarioPathWith(policy: (Request) -> String): Builder =
            apply { filingPolicy = FilingPolicyBuilder(policy) }

        private class FilingPolicyBuilder(private val policy: (Request) -> String) : FilingPolicy {
            override fun getPath(request: Request): String = policy(request)
        }

        /**
         * For static mocks: Defines a loading function to retrieve the scenario files as a stream
         * @param loading a function to load files by name and path as a stream (could use
         * Android's assets.open, Classloader.getRessourceAsStream, FileInputStream, etc.)
         */
        fun loadFileWith(loading: LoadFile): Builder = apply { openFile = loading }

        /**
         * Uses dynamic mocks to answer network requests instead of file scenarios
         * @param callback A callback to invoke when a request in intercepted
         */
        fun useDynamicMocks(callback: RequestCallback): Builder =
            apply { dynamicCallbacks += callback }

        /**
         * Uses dynamic mocks to answer network requests instead of file scenarios
         * @param callback A callback to invoke when a request in intercepted: must return a
         * ResponseDescriptor for the current Request or null if not suitable Response could be
         * computed
         */
        fun useDynamicMocks(callback: (Request) -> ResponseDescriptor?): Builder =
            useDynamicMocks(CallBackBuilder(callback))

        private class CallBackBuilder(
            private val block: (Request) -> ResponseDescriptor?
        ) : RequestCallback {
            override fun loadResponse(request: Request): ResponseDescriptor? = block(request)
        }

        /**
         * Defines the mapper to use to parse the scenario files (Jackson, Moshi, GSON...)
         * @param objectMapper A Mapper to parse scenario files.
         */
        fun parseScenariosWith(objectMapper: Mapper): Builder = apply { mapper = objectMapper }

        /**
         * Defines the folder where scenarios should be stored when recording
         * @param folder the root folder where saved scenarios should be saved
         */
        fun saveScenariosIn(folder: File): Builder = apply { root = folder }

        /**
         * Allows to return an error if saving fails when recording.
         * @param failOnError if true, failure to save scenarios will throw an exception.
         * If false, saving exceptions will be ignored.
         */
        fun failOnRecordingError(failOnError: Boolean): Builder =
            apply { showSavingErrors = failOnError }

        /**
         * Allows to set a fake delay for every requests (can be overridden in a scenario) to
         * achieve a more realistic behavior (probably necessary if you want to display loading
         * animations during your network calls).
         * @param delay default pause delay for network responses in ms
         */
        fun addFakeNetworkDelay(delay: Long): Builder = apply { simulatedDelay = delay }

        /**
         * Defines how the interceptor should initially behave (can be enabled, disable, record
         * requests...)
         * @param status The interceptor mode
         */
        fun setInterceptorStatus(status: Mode): Builder = apply { interceptorMode = status }

        /**
         * Builds the interceptor.
         */
        fun build(): MockResponseInterceptor = MockResponseInterceptor(
            buildProviders(),
            mapper?.let { RequestRecorder(it, filingPolicy, root, showSavingErrors) }
        ).apply {
            if (interceptorMode == Mode.RECORD && root == null) {
                error(NO_ROOT_FOLDER_ERROR)
            }
            delay = simulatedDelay
            mode = interceptorMode
        }

        private fun buildProviders(): List<ScenarioProvider> {
            val dynamicMockProvider =
                dynamicCallbacks.takeIf { it.isNotEmpty() }?.let { DynamicMockProvider(it) }
            val staticMockProvider = buildStaticProvider()
            return listOfNotNull(dynamicMockProvider, staticMockProvider)
        }

        private fun buildStaticProvider(): StaticMockProvider? = mapper?.let { jsonMapper ->
            openFile?.let { fileLoading ->
                StaticMockProvider(filingPolicy, fileLoading, jsonMapper)
            }
        }
    }
}

/**
 * A loading function that takes a path as input and returns an InputStream to read from. Typical
 * implementations can use FileInputStream instantiations, Classloader.getResourceAsStream call or
 * use of the AssetManager on Android.
 */
typealias LoadFile = (String) -> InputStream?

const val RECORD_NOT_SUPPORTED_ERROR =
    "Recording is not supported with the current parameters."

const val NO_RECORDER_ERROR =
    "Recording configuration is not complete. Please add a Mapper."

const val NO_ROOT_FOLDER_ERROR =
    "Network calls can not be recorded without a folder where to save files. Please add a root folder."

private val HTTP_RESPONSES_CODE: Map<Int, String> = mapOf(
    200 to "OK",
    201 to "Created",
    202 to "Accepted",
    203 to "Non-Authoritative Information",
    204 to "No Content",
    205 to "Reset Content",
    206 to "Partial Content",
    207 to "Multi-Status",
    208 to "Already Reported",
    210 to "Content Different",
    226 to "IM Used",
    300 to "Multiple Choices",
    301 to "Moved Permanently",
    302 to "Found",
    303 to "See Other",
    304 to "Not Modified",
    305 to "Use Proxy",
    306 to "Switch Proxy",
    307 to "Temporary Redirect",
    308 to "Permanent Redirect",
    310 to "Too many Redirects",
    400 to "Bad Request",
    401 to "Unauthorized",
    402 to "Payment Required",
    403 to "Forbidden",
    404 to "Not Found",
    405 to "Method Not Allowed",
    406 to "Not Acceptable",
    407 to "Proxy Authentication Required",
    408 to "Request Time-out",
    409 to "Conflict",
    410 to "Gone",
    411 to "Length Required",
    412 to "Precondition Failed",
    413 to "Request Entity Too Large",
    414 to "Request-URI Too Long",
    415 to "Unsupported Media Type",
    416 to "Requested range unsatisfiable",
    417 to "Expectation failed",
    418 to "I’m a teapot",
    421 to "Bad mapping / Misdirected Request",
    422 to "Unprocessable entity",
    423 to "Locked",
    424 to "Method failure",
    425 to "Unordered Collection",
    426 to "Upgrade Required",
    428 to "Precondition Required",
    429 to "Too Many Requests",
    431 to "Request Header Fields Too Large",
    444 to "No Response",
    449 to "Retry With",
    450 to "Blocked by Windows Parental Controls",
    451 to "Unavailable For Legal Reasons",
    456 to "Unrecoverable Error",
    495 to "SSL Certificate Error",
    496 to "SSL Certificate Required",
    497 to "HTTP Request Sent to HTTPS Port",
    498 to "Token expired/invalid",
    499 to "Client Closed Request",
    500 to "Internal Server Error",
    501 to "Not Implemented",
    502 to "Bad Gateway",
    503 to "Service unavailable",
    504 to "Gateway Time-out",
    505 to "HTTP Version not supported",
    506 to "Variant Also Negotiates",
    507 to "Insufficient storage",
    508 to "Loop detected",
    509 to "Bandwidth Limit Exceeded",
    510 to "Not extended",
    511 to "Network authentication required",
    520 to "Unknown Error",
    521 to "Web Server Is Down",
    522 to "Connection Timed Out",
    523 to "Origin Is Unreachable",
    524 to "A Timeout Occurred",
    525 to "SSL Handshake Failed",
    526 to "Invalid SSL Certificate",
    527 to "Railgun Error"
)
