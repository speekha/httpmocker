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

package fr.speekha.httpmocker.policies

import okhttp3.Request

/**
 * Simple filing policy that stores all the configuration files in the same folder. Slashes in the URL are replaced
 * by underscores.
 */
class SingleFolderPolicy(private val rootFolder: String = "") : FilingPolicy {

    override fun getPath(request: Request): String {
        val prefix = if (rootFolder.isEmpty()) "" else "$rootFolder/"
        val fileName = request.url()
            .pathSegments()
            .filter { it.isNotEmpty() }
            .joinToString("_")
            .takeIf { it.isNotBlank() }
            ?: "index"
        return "$prefix$fileName.json"
    }

}