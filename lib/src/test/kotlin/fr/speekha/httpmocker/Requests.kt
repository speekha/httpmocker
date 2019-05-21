package fr.speekha.httpmocker

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

fun buildRequest(
    url: String,
    headers: List<Pair<String, String>> = emptyList(),
    method: String = "GET",
    body: String? = null
): Request {
    return Request.Builder()
        .url(url)
        .headers(Headers.of(*headers.flatMap { listOf(it.first, it.second) }.toTypedArray()))
        .method(method, body?.let { RequestBody.create(MediaType.parse("text/plain"), it) })
        .build()
}