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

package fr.speekha.httpmocker.builder

import java.io.File
import java.io.InputStream

fun mockInterceptor(assemble: InterceptorBuilder.() -> Unit) = with(
    InterceptorBuilder()
) {
    assemble()
    build()
}

fun InterceptorBuilder.recordScenariosIn(folder: File): RecorderBuilder =
    RecorderBuilder(folder).also { recorder = it }

fun InterceptorBuilder.recordScenariosIn(folder: String): RecorderBuilder =
    recordScenariosIn(File(folder))

/**
 * A loading function that takes a path as input and returns an InputStream to read from. Typical
 * implementations can use FileInputStream instantiations, Classloader.getResourceAsStream call or
 * use of the AssetManager on Android.
 */
typealias LoadFile = (String) -> InputStream?
