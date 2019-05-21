package fr.speekha.httpmocker.policies

import okhttp3.Request

interface FilingPolicy {

    /**
     * Computes the path where the appropriate configuration file should be for this request.
     * @param request the intercepted OkHttpRequest
     * @return the path where the JSON configuration can be loaded
     */
    fun getPath(request: Request): String

}
