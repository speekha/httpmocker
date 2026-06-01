/*
 * Copyright 2019-2026 David Blanc
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

package fr.speekha.httpmocker

import fr.speekha.httpmocker.model.NetworkError

actual object ClassloaderUtils {

    // Registry of exception factory functions (populated during app initialization)
    // Maps exception type name to a factory function that creates the exception
    private val exceptionRegistry: MutableMap<String, (String?) -> Throwable> = mutableMapOf()

    @Suppress("TooGenericExceptionCaught")
    actual fun createException(error: NetworkError): Throwable {
        val factory = exceptionRegistry[error.exceptionType]
        return if (factory != null) {
            try {
                factory(error.message)
            } catch (e: Throwable) {
                RuntimeException(error.message, e)
            }
        } else {
            RuntimeException("${error.exceptionType}: ${error.message}")
        }
    }

    /**
     * Register an exception factory for use in createException.
     * Must be called during app initialization to support dynamic exception creation.
     * 
     * Example:
     * ```
     * ClassloaderUtils.registerException(
     *     "java.io.IOException", 
     *     { message -> IOException(message) }
     * )
     * ```
     */
    fun registerException(typeName: String, factory: (String?) -> Throwable) {
        exceptionRegistry[typeName] = factory
    }
}
