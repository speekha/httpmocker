/*
 * Copyright 2019-2021 David Blanc
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

    actual fun debug(message: String) {
        printMessage(message)
    }

    actual fun info(message: String) {
        printMessage(message)
    }

    actual fun warn(message: String) {
        printMessage(message)
    }

    actual fun error(message: String, exception: Throwable?) {
        printMessage(message, exception)
    }

    private fun printMessage(message: String, exception: Throwable? = null) {
        println(message)
        if (exception != null) {
            println("Exception: $exception")
        }
    }
}
