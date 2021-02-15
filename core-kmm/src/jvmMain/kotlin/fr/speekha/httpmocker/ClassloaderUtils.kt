/*
 *  Copyright 2019-2021 David Blanc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package fr.speekha.httpmocker

import fr.speekha.httpmocker.io.readAsStringList
import fr.speekha.httpmocker.model.NetworkError

actual class ClassloaderUtils {
    actual fun createException(error: NetworkError): Throwable {
        val exceptionType = Class.forName(error.exceptionType)
        return if (error.message == null) {
            exceptionType.newInstance()
        } else {
            exceptionType.getConstructor(String::class.java).newInstance(error.message)
        } as Throwable
    }

    actual fun loadExtensionMap(): Map<String, String> =
        javaClass.classLoader.getResourceAsStream("fr/speekha/httpmocker/resources/mimetypes")
            ?.readAsStringList()
            ?.associate {
                val (extension, mimeType) = it.split("=")
                mimeType to extension
            } ?: mapOf()

}