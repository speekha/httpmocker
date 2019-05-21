package fr.speekha.httpmocker.policies

import okhttp3.Request

/**
 * Simple filing policy discarding host name, and matching file path with url path.
 */
class MirrorPathPolicy : FilingPolicy {

    override fun getPath(request: Request): String = request.url().pathSegments().joinToString("/") + ".json"

}