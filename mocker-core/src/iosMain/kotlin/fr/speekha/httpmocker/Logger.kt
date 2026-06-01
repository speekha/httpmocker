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

import kotlin.reflect.KClass

actual class Logger actual constructor(clazz: KClass<*>) {

    private val className = clazz.simpleName ?: "Unknown"

    actual fun debug(message: String) {
        println("[DEBUG] $className: $message")
    }

    actual fun info(message: String) {
        println("[INFO] $className: $message")
    }

    actual fun warn(message: String) {
        println("[WARN] $className: $message")
    }

    actual fun error(message: String, exception: Throwable?) {
        if (exception != null) {
            println("[ERROR] $className: $message - ${exception.message}")
        } else {
            println("[ERROR] $className: $message")
        }
    }
}
