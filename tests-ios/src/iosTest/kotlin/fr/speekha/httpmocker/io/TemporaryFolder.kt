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

package fr.speekha.httpmocker.io

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * Creates a unique empty folder under the simulator's temporary directory, runs [block] with it,
 * then deletes it.
 */
@OptIn(ExperimentalForeignApi::class)
fun withTemporaryFolder(name: String, block: (FileAccessor) -> Unit) {
    val folder = createFileAccessor("${NSTemporaryDirectory()}httpmocker-$name-${NSUUID().UUIDString}")
    folder.mkdir()
    try {
        block(folder)
    } finally {
        NSFileManager.defaultManager().removeItemAtPath(folder.absolutePath, error = null)
    }
}

fun FileAccessor.writeText(text: String) {
    getWriter().use { it.write(text.encodeToByteArray()) }
}
