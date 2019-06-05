package fr.speekha.httpmocker.policies

import okhttp3.Request

/**
 * Simple filing policy that uses the complete URL, including host name, to retrieve configuration files.
 */
class ServerSpecificPolicy : FilingPolicy {
    override fun getPath(request: Request): String {
        val url = request.url()
        return (listOf(url.host()) + url.pathSegments()).joinToString("/") + ".json"
    }
}