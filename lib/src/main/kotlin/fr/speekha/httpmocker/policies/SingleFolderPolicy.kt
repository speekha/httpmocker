package fr.speekha.httpmocker.policies

import okhttp3.Request

class SingleFolderPolicy(private val rootFolder: String = "") : FilingPolicy {

    override fun getPath(request: Request): String {
        val fileName = request.url().pathSegments().joinToString("_")
        return if (rootFolder.isEmpty()) "$fileName.json" else "$rootFolder/$fileName.json"
    }

}