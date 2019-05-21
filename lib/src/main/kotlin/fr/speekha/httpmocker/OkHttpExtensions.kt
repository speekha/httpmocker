package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.Header
import fr.speekha.httpmocker.model.RequestDescriptor
import fr.speekha.httpmocker.model.ResponseDescriptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer

fun RequestBody.readAsString(): String? = Buffer().let {
    writeTo(it)
    it.inputStream().bufferedReader().use { reader -> reader.readText() }
}

fun Request.matchBody(request: RequestDescriptor): Boolean = request.body?.let { bodyPattern ->
    val requestBody = body()?.readAsString()
    requestBody != null && Regex(bodyPattern).matches(requestBody)
} ?: true

fun Request.toDescriptor() = RequestDescriptor(
    method = method(),
    body = body()?.readAsString(),
    params = url().queryParameterNames().associate { it to (url().queryParameter(it) ?: "") },
    headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
)

fun Response.toDescriptor(duplicates: Int, extension: String) = ResponseDescriptor(
    code = code(),
    bodyFile = request().url().pathSegments().last() + "_body_$duplicates$extension",
    headers = headers().names().flatMap { name -> headers(name).map { Header(name, it) } }
)

fun Response.copyResponse(body: ByteArray?): Response = newBuilder()
    .body(ResponseBody.create(body()?.contentType(), body ?: byteArrayOf()))
    .build()
