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

expect class Logger(clazz: KClass<*>) {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, exception: Throwable? = null)
}

inline fun <reified T : Any> T.getLogger(): Logger = Logger(this::class)
