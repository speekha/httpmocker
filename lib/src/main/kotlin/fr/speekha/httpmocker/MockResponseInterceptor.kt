package fr.speekha.httpmocker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import fr.speekha.httpmocker.model.*
import fr.speekha.httpmocker.policies.FilingPolicy
import okhttp3.*
import okio.Buffer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset

/**
 * A OkHTTP interceptor that can let requests through or block them and answer them with predefined responses.
 * Genuine network connections can also be recorded to create reusable offline scenarios.
 * @param filingPolicy The policy used to retrieve the configuration files based on the request being intercepted
 * @param openFile A loading function to retrieve the JSON configuration as a stream
 * @param rootFolder The root folder where scenarios should be stored when recording
 */
class MockResponseInterceptor(
    private val filingPolicy: FilingPolicy,
    private val openFile: LoadFile,
    private val rootFolder: File? = null
) : Interceptor {

    /**
     * An arbitrary delay to include when answering requests in order to have a realistic behavior (GUI can display
     * loaders, etc.)
     */
    var delay: Long = 0

    /**
     * Enables to set the interception mode:
     * DISABLED lets every request through without interception.
     * ENABLED intercepts all requests and return responses found in a predefined configuration
     * MIXED allows to look for responses locally, but execute the request if no response is found
     */
    var mode: MODE = MODE.DISABLED

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val extensionMappings: Map<String, String> by lazy { loadExtensionMap() }

    private fun loadExtensionMap(): Map<String, String> = mapper.readValue<List<Extensions>>(
        javaClass.classLoader.getResourceAsStream("fr/speekha/httpmocker/resources/mimetypes.json"),
        jacksonTypeRef<List<Extensions>>()
    ).associate { it.mimeType to it.extension }

    override fun intercept(chain: Interceptor.Chain): Response = when (mode) {
        MODE.DISABLED -> proceedWithRequest(chain)
        MODE.ENABLED -> mockResponse(chain.request()) ?: buildResponse(chain.request(), responseNotFound())
        MODE.MIXED -> mockResponse(chain.request()) ?: proceedWithRequest(chain)
        MODE.RECORD -> recordCall(chain)
    }

    private fun proceedWithRequest(chain: Interceptor.Chain) = chain.proceed(chain.request())

    private fun mockResponse(request: Request): Response? = loadResponse(request)?.let { response ->
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
        buildResponse(request, response)
    }

    private fun loadResponse(request: Request): ResponseDescriptor? = try {
        openFile(filingPolicy.getPath(request))?.let { stream ->
            val list = mapper.readValue<List<Matcher>>(stream, jacksonTypeRef<List<Matcher>>())
            matchRequest(request, list)
        }
    } catch (e: Throwable) {
        null
    }

    private fun buildResponse(request: Request, response: ResponseDescriptor): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(response.code)
        .message(messageForHttpCode(response.code))
        .body(loadBody(request, response))
        .apply {
            response.headers.forEach {
                header(it.name, it.value)
            }
        }
        .build()

    private fun responseNotFound() = ResponseDescriptor(code = 404, body = "Page not found")

    private fun loadBody(request: Request, response: ResponseDescriptor) =
        ResponseBody.create(MediaType.parse(response.mediaType), response.bodyFile?.let {
            val responsePath = filingPolicy.getPath(request)
            val bodyPath = responsePath.substring(0, responsePath.lastIndexOf('/') + 1) + it
            openFile(bodyPath)?.readBytes()
        } ?: response.body.toByteArray(Charset.forName("UTF-8")))

    private fun matchRequest(request: Request, list: List<Matcher>): ResponseDescriptor? =
        list.firstOrNull { it.request.match(request) }?.response

    private fun RequestDescriptor.match(request: Request): Boolean =
        (method?.let { it.toUpperCase() == request.method() } ?: true) &&
                headers.all { request.headers(it.name).contains(it.value) } &&
                params.all { request.url().queryParameter(it.key) == it.value } &&
                matchRequestBody(request)

    private fun RequestDescriptor.matchRequestBody(request: Request): Boolean {
        return body?.let { bodyPattern ->
            val requestBody = request.readBody()
            requestBody != null && Regex(bodyPattern).matches(requestBody)
        } ?: true
    }

    private fun recordCall(chain: Interceptor.Chain): Response {
        val response = proceedWithRequest(chain)
        val body = response.body()?.bytes()
        saveFiles(chain.request(), response, body)
        return response.copyResponse(body)
    }

    private fun saveFiles(request: Request, response: Response, body: ByteArray?) = try {
        val storeFile = filingPolicy.getPath(request)
        val matchers = createMatcher(storeFile, request, response)
        val requestFile = File(rootFolder, storeFile)

        saveRequestFile(requestFile, matchers)

        matchers.last().response.bodyFile?.let { responseFile ->
            saveResponseBody(File(requestFile.parentFile, responseFile), body)
        }
    } catch (e: Throwable) {
    }

    private fun saveResponseBody(storeFile: File, body: ByteArray?) = openFile(storeFile).use {
        it.write(body)
    }

    private fun createMatcher(storeFile: String, request: Request, response: Response): List<Matcher> {
        val requestFile = File(rootFolder, storeFile)
        val previousRecords: List<Matcher> = if (requestFile.exists())
            mapper.readValue<List<Matcher>>(requestFile, jacksonTypeRef<List<Matcher>>()).toMutableList()
        else emptyList()
        return previousRecords + Matcher(request.toDescriptor(), response.toDescriptor(previousRecords.size))
    }

    private fun saveRequestFile(requestFile: File, matchers: List<Matcher>) {
        openFile(requestFile).use {
            mapper.writeValue(it, matchers)
        }
    }

    private fun openFile(file: File): FileOutputStream {
        createParent(file.parentFile)
        return FileOutputStream(file)
    }

    private fun createParent(file: File?) {
        if (file?.parentFile?.exists() == false) {
            createParent(file.parentFile)
            file.mkdir()
        } else if (file?.exists() == false) {
            file.mkdir()
        }
    }

    private fun Request.readBody(): String? = body()?.let {
        val sink = Buffer()
        it.writeTo(sink)
        return sink.inputStream().bufferedReader().use { reader -> reader.readText() }
    }

    private fun Request.toDescriptor() = RequestDescriptor(
        method = method(),
        body = readBody(),
        params = url().queryParameterNames().associate { it to (url().queryParameter(it) ?: "") },
        headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
    )

    private fun Response.toDescriptor(duplicates: Int) = ResponseDescriptor(
        code = code(),
        bodyFile = request().url().pathSegments().last() + "_body_$duplicates${getExtension(body()?.contentType())}",
        headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
    )

    private fun Response.copyResponse(body: ByteArray?): Response = newBuilder()
        .body(ResponseBody.create(body()?.contentType(), body ?: byteArrayOf()))
        .build()

    private fun messageForHttpCode(httpCode: Int) = HTTP_RESPONSES_CODE[httpCode] ?: error("Unknown error code")

    private fun getExtension(contentType: MediaType?) = extensionMappings[contentType.toString()] ?: ".txt"

    companion object {
        val HTTP_RESPONSES_CODE = mapOf(
            200 to "OK",
            201 to "Created",
            204 to "No Content",
            302 to "Found",
            400 to "Bad Request",
            403 to "Forbidden",
            404 to "Not Found",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service unavailable"
        )
    }

    enum class MODE {
        DISABLED, ENABLED, MIXED, RECORD
    }
}

/**
 * A loading function that takes a path as input and returns an InputStream to read from. Typical implementations can use
 * FileInputStream instantiations, Classloader.getResourceAsStream call or use of the AssetManager on Android.
 */
typealias LoadFile = (String) -> InputStream?
