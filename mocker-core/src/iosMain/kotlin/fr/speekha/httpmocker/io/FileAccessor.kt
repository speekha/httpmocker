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
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
actual class FileAccessor actual constructor(
    path: String
) {

    private val fileManager = NSFileManager.defaultManager()
    private val url = NSURL(fileURLWithPath = path)

    actual val name: String
        get() = url.lastPathComponent ?: ""

    actual val parentFile: FileAccessor?
        get() = url.URLByDeletingLastPathComponent?.path?.let { FileAccessor(it) }

    actual val absolutePath: String
        get() = url.path ?: ""

    actual fun getFile(fileName: String): FileAccessor {
        val childUrl = url.URLByAppendingPathComponent(fileName)
        return FileAccessor(childUrl?.path ?: "")
    }

    actual fun exists(): Boolean {
        return fileManager.fileExistsAtPath(absolutePath)
    }

    actual fun mkdir() {
        fileManager.createDirectoryAtPath(
            absolutePath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    actual fun getReader(): StreamReader {
        val data = fileManager.contentsAtPath(absolutePath)
            ?: throw IOException("Unable to read file: $absolutePath")
        return StreamReader(data)
    }

    actual fun getWriter(): StreamWriter {
        return StreamWriter(absolutePath)
    }
}
