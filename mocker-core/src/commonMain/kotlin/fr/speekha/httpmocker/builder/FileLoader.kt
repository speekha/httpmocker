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

package fr.speekha.httpmocker.builder

import fr.speekha.httpmocker.io.StreamReader

/**
 * A loading function that takes a path as input and returns an InputStream to read from. Typical
 * implementations can use FileInputStream instantiations, Classloader.getResourceAsStream call or
 * use of the AssetManager on Android.
 */
fun interface FileLoader {
    /**
     * The method to load scenario files.
     */
    fun load(file: String): StreamReader?
}

internal expect fun wrapLoadingExceptions(loader: FileLoader): FileLoader
