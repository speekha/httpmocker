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

package fr.speekha.httpmocker.builder

import fr.speekha.httpmocker.io.IOException
import java.io.FileNotFoundException

internal actual fun wrapLoadingExceptions(loader: FileLoader): FileLoader = FileLoader { path ->
    try {
        loader.load(path)
    } catch (e: FileNotFoundException) {
        throw IOException(e.message, e)
    }
}