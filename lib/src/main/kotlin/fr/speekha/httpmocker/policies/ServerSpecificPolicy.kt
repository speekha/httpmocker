package fr.speekha.httpmocker.policies

import okhttp3.Request

class ServerSpecificPolicy : FilingPolicy {
    override fun getPath(request: Request): String {
        val url = request.url()
        return (listOf(url.host()) + url.pathSegments()).joinToString("/") + ".json"
    }
}