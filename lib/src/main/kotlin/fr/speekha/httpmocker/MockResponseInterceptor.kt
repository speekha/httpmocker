package fr.speekha.httpmocker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import fr.speekha.httpmocker.model.Matcher
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.*
import okio.Buffer
import java.io.InputStream
import java.nio.charset.Charset

class MockResponseInterceptor(
    val openFile: (String) -> InputStream?
) : Interceptor {

    var delay: Long = 0

    var enabled: Boolean = false

    val mapper: ObjectMapper = jacksonObjectMapper()

    override fun intercept(chain: Interceptor.Chain): Response = if (enabled) {
        val request = chain.request()
        val response = loadResponse(request)
        when {
            response.delay > 0 -> Thread.sleep(response.delay)
            delay > 0 -> Thread.sleep(delay)
        }
        buildResponse(request, response)
    } else {
        chain.proceed(chain.request())
    }

    private fun buildResponse(request: Request, response: ResponseDescriptor): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(response.code)
        .message(messageForHttpCode(response.code))
        .body(loadBody(response))
        .apply {
            response.headers.forEach {
                header(it.name, it.value)
            }
        }
        .build()

    private fun loadBody(response: ResponseDescriptor) =
        ResponseBody.create(MediaType.parse(response.mediaType), response.bodyFile?.let {
            openFile(it)?.readBytes()
        } ?: response.body.toByteArray(Charset.forName("UTF-8")))

    private fun loadResponse(request: Request): ResponseDescriptor = try {
        val url = request.url()
        val path = url.encodedPath() + ".json"
        openFile(path.drop(1))?.let { stream ->
            val list = mapper.readValue<List<Matcher>>(stream, jacksonTypeRef<List<Matcher>>())
            matchRequest(request, list)
        } ?: responseNotFound()
    } catch (e: Throwable) {
        responseNotFound()
    }

    private fun responseNotFound() = ResponseDescriptor(
        code = 404,
        body = "Page not found"
    )

    private fun matchRequest(request: Request, list: List<Matcher>): ResponseDescriptor? =
        list.firstOrNull { it.request.match(request) }?.response

    private fun RequestDescriptor.match(request: Request): Boolean =
        (method?.let { it.toUpperCase() == request.method() } ?: true) &&
                headers.all { request.headers(it.name).contains(it.value) } &&
                params.all { request.url().queryParameter(it.key) == it.value } &&
                matchRequestBody(request)

    private fun RequestDescriptor.matchRequestBody(request: Request): Boolean {
        return body?.let { bodyPattern ->
            val sink = Buffer()
            request.body()?.writeTo(sink)
            val requestBody = sink.inputStream().bufferedReader().use { reader -> reader.readText() }
            Regex(bodyPattern).matches(requestBody)
        } ?: true
    }

    private fun messageForHttpCode(httpCode: Int) = HTTP_RESPONSES_CODE[httpCode] ?: error("Unknown error code")

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
}
